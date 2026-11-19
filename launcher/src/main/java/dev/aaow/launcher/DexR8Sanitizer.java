package dev.aaow.launcher;

import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.DexLabel;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexCodeNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.node.TryCatchNode;
import com.googlecode.d2j.node.insn.AbstractMethodStmtNode;
import com.googlecode.d2j.node.insn.BaseSwitchStmtNode;
import com.googlecode.d2j.node.insn.ConstStmtNode;
import com.googlecode.d2j.node.insn.DexLabelStmtNode;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.FieldStmtNode;
import com.googlecode.d2j.node.insn.FillArrayDataStmtNode;
import com.googlecode.d2j.node.insn.FilledNewArrayStmtNode;
import com.googlecode.d2j.node.insn.JumpStmtNode;
import com.googlecode.d2j.node.insn.MethodCustomStmtNode;
import com.googlecode.d2j.node.insn.MethodPolymorphicStmtNode;
import com.googlecode.d2j.node.insn.MethodStmtNode;
import com.googlecode.d2j.node.insn.PackedSwitchStmtNode;
import com.googlecode.d2j.node.insn.SparseSwitchStmtNode;
import com.googlecode.d2j.node.insn.Stmt0RNode;
import com.googlecode.d2j.node.insn.Stmt1RNode;
import com.googlecode.d2j.node.insn.Stmt2R1NNode;
import com.googlecode.d2j.node.insn.Stmt2RNode;
import com.googlecode.d2j.node.insn.Stmt3RNode;
import com.googlecode.d2j.node.insn.TypeStmtNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.Op;
import com.googlecode.d2j.visitors.DexFileVisitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Bộ chuẩn hóa DEX trước khi đưa sang {@code dex2jar} nhằm khôi phục các tối ưu hóa của R8/D8
 * vốn hợp lệ trên ART/Dalvik nhưng vi phạm JVM Bytecode Verifier (HotSpot Java 21):
 * <ol>
 *   <li><b>R8 Constructor Inlining &amp; Dead Constructor Removal:</b> R8 thường xóa constructor của lớp con
 *       (như {@code androidx.activity.contextaware.ContextAwareHelper} hay {@code androidx.startup.StartupException})
 *       và phát lệnh {@code new-instance Sub} rồi gọi thẳng {@code invoke-direct Super.&lt;init&gt;} (hoặc gọi nhảy cóc
 *       lên lớp ông trong {@code this.&lt;init&gt;}). Trên {@code dex2jar}, cặp lệnh này bị dịch sai thành
 *       {@code new java/lang/Object} hoặc {@code invokespecial Super.&lt;init&gt;} trên đối tượng {@code uninitialized(Sub)},
 *       gây {@code VerifyError} ngay khi nạp lớp.</li>
 *   <li>Bộ chuẩn hóa này theo dõi thanh ghi {@code uninitialized} bằng phân tích luồng dữ liệu (dataflow),
 *       tự động tổng hợp chuỗi bridge constructor hợp lệ trên từng lớp trung gian trong APK và viết lại
 *       lời gọi {@code invoke-direct} về đúng lớp đích trước khi {@code dex2jar} sinh bytecode JVM.</li>
 * </ol>
 */
public final class DexR8Sanitizer {
    private static final String MARKER_DESC = "Ldev/aaow/rt/R8CtorMarker;";

    public record SanitizeStats(int rewrittenCallSites, int synthesizedConstructors) {
    }

    public record SanitizedReader(BaseDexFileReader reader, SanitizeStats stats) {
    }

    private record UninitRef(String typeDesc, int allocSiteIndex, boolean isThis) {
    }

    private record RewriteTarget(int stmtIndex, MethodStmtNode origStmt, String requiredOwner) {
    }

    private record BridgeKey(String ownerDesc, String superTargetOwner, String superTargetDesc) {
    }

    private DexR8Sanitizer() {
    }

    /**
     * Đọc toàn bộ DEX từ {@code rawReader}, chuẩn hóa các lời gọi constructor đã bị R8 inline,
     * và trả về {@link BaseDexFileReader} mới sẵn sàng truyền thẳng cho {@code Dex2jar.from(...)}.
     */
    public static SanitizedReader sanitize(BaseDexFileReader rawReader) {
        DexFileNode fileNode = new DexFileNode();
        rawReader.accept(fileNode, 0);
        SanitizeStats stats = sanitizeInPlace(fileNode);
        BaseDexFileReader wrapped = new DexFileNodeReader(fileNode, rawReader.getDexVersion());
        return new SanitizedReader(wrapped, stats);
    }

    /**
     * Chuẩn hóa trực tiếp trên {@link DexFileNode} (hữu ích cho cả pipeline chính lẫn unit test).
     */
    public static SanitizeStats sanitizeInPlace(DexFileNode fileNode) {
        if (fileNode == null || fileNode.clzs == null || fileNode.clzs.isEmpty()) {
            return new SanitizeStats(0, 0);
        }

        Map<String, DexClassNode> classMap = new HashMap<>(fileNode.clzs.size() * 2);
        Map<String, Set<String>> existingCtors = new HashMap<>(fileNode.clzs.size() * 2);
        for (DexClassNode cls : fileNode.clzs) {
            if (cls == null || cls.className == null) continue;
            classMap.put(cls.className, cls);
            Set<String> ctorDescs = new HashSet<>();
            if (cls.methods != null) {
                for (DexMethodNode m : cls.methods) {
                    if (m != null && m.method != null && "<init>".equals(m.method.getName())) {
                        ctorDescs.add(m.method.getDesc());
                    }
                }
            }
            existingCtors.put(cls.className, ctorDescs);
        }

        Map<BridgeKey, Method> bridgeCache = new HashMap<>();
        int[] synthCount = new int[]{0};
        int rewrittenCount = 0;

        for (DexClassNode cls : fileNode.clzs) {
            if (cls == null || cls.methods == null || cls.methods.isEmpty()) continue;
            List<DexMethodNode> snapshot = new ArrayList<>(cls.methods);
            for (DexMethodNode method : snapshot) {
                if (method == null || method.codeNode == null || method.codeNode.stmts == null
                        || method.codeNode.stmts.isEmpty()) {
                    continue;
                }
                List<RewriteTarget> targets = analyzeMethod(cls, method);
                if (targets.isEmpty()) continue;

                rewrittenCount += applyRewrites(method.codeNode, targets, classMap, existingCtors, bridgeCache, synthCount);
            }
        }

        return new SanitizeStats(rewrittenCount, synthCount[0]);
    }

    private static List<RewriteTarget> analyzeMethod(DexClassNode cls, DexMethodNode method) {
        DexCodeNode code = method.codeNode;
        List<DexStmtNode> stmts = code.stmts;
        int n = stmts.size();

        // Lọc nhanh: chỉ chạy dataflow nếu phương thức vừa có INVOKE_DIRECT* gọi <init>,
        // vừa là <init> (có thể gọi super nhảy cóc) hoặc có NEW_INSTANCE trong thân hàm.
        boolean isCtor = "<init>".equals(method.method.getName()) && (method.access & DexConstants.ACC_STATIC) == 0;
        boolean hasInitInvoke = false;
        boolean hasNewInstance = false;
        for (int i = 0; i < n; i++) {
            DexStmtNode s = stmts.get(i);
            if (s instanceof MethodStmtNode ms
                    && (ms.op == Op.INVOKE_DIRECT || ms.op == Op.INVOKE_DIRECT_RANGE)
                    && ms.method != null && "<init>".equals(ms.method.getName())) {
                hasInitInvoke = true;
            } else if (s instanceof TypeStmtNode ts && ts.op == Op.NEW_INSTANCE) {
                hasNewInstance = true;
            }
            if (hasInitInvoke && (isCtor || hasNewInstance)) {
                break;
            }
        }
        if (!hasInitInvoke || (!isCtor && !hasNewInstance)) {
            return Collections.emptyList();
        }

        Map<DexLabel, Integer> labelToIdx = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (stmts.get(i) instanceof DexLabelStmtNode ls && ls.label != null) {
                labelToIdx.put(ls.label, i);
            }
        }

        // Xây dựng try-catch edges theo từng vị trí lệnh nếu có TryCatchNode.
        Map<Integer, List<Integer>> tryHandlerMap = Collections.emptyMap();
        if (code.tryStmts != null && !code.tryStmts.isEmpty()) {
            tryHandlerMap = new HashMap<>();
            for (TryCatchNode tc : code.tryStmts) {
                Integer startIdx = labelToIdx.get(tc.start);
                Integer endIdx = labelToIdx.get(tc.end);
                if (startIdx == null || endIdx == null || tc.handler == null) continue;
                List<Integer> hIdxs = new ArrayList<>();
                for (DexLabel h : tc.handler) {
                    Integer hi = labelToIdx.get(h);
                    if (hi != null) hIdxs.add(hi);
                }
                if (hIdxs.isEmpty()) continue;
                for (int i = startIdx; i < endIdx && i < n; i++) {
                    tryHandlerMap.computeIfAbsent(i, k -> new ArrayList<>()).addAll(hIdxs);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<Integer, UninitRef>[] inStates = new Map[n];
        Map<Integer, UninitRef> entry = new HashMap<>();
        if ("<init>".equals(method.method.getName()) && (method.access & DexConstants.ACC_STATIC) == 0) {
            int paramSlots = computeParamSlots(method.method.getParameterTypes(), false);
            int thisReg = code.totalRegister - paramSlots;
            if (thisReg >= 0) {
                entry.put(thisReg, new UninitRef(cls.className, -1, true));
            }
        }
        inStates[0] = entry;

        Queue<Integer> worklist = new ArrayDeque<>();
        boolean[] inQueue = new boolean[n];
        worklist.add(0);
        inQueue[0] = true;

        int maxSteps = Math.max(256, n * 16);
        int steps = 0;
        while (!worklist.isEmpty() && steps++ < maxSteps) {
            int idx = worklist.poll();
            inQueue[idx] = false;
            Map<Integer, UninitRef> cur = inStates[idx];
            if (cur == null) continue;

            DexStmtNode stmt = stmts.get(idx);
            Map<Integer, UninitRef> out = transfer(stmt, idx, cur);

            // Truyền sang các exception handler nếu lệnh nằm trong khối try
            List<Integer> handlers = tryHandlerMap.get(idx);
            if (handlers != null) {
                for (int hIdx : handlers) {
                    if (mergeInto(inStates, hIdx, cur) && !inQueue[hIdx]) {
                        worklist.add(hIdx);
                        inQueue[hIdx] = true;
                    }
                }
            }

            // Tính toán successor CFG
            if (stmt instanceof JumpStmtNode js) {
                Integer targetIdx = labelToIdx.get(js.label);
                if (targetIdx != null && mergeInto(inStates, targetIdx, out) && !inQueue[targetIdx]) {
                    worklist.add(targetIdx);
                    inQueue[targetIdx] = true;
                }
                boolean isUnconditionalGoto = js.op == Op.GOTO || js.op == Op.GOTO_16 || js.op == Op.GOTO_32;
                if (!isUnconditionalGoto && idx + 1 < n) {
                    if (mergeInto(inStates, idx + 1, out) && !inQueue[idx + 1]) {
                        worklist.add(idx + 1);
                        inQueue[idx + 1] = true;
                    }
                }
            } else if (stmt instanceof BaseSwitchStmtNode sw) {
                if (sw.labels != null) {
                    for (DexLabel lbl : sw.labels) {
                        Integer t = labelToIdx.get(lbl);
                        if (t != null && mergeInto(inStates, t, out) && !inQueue[t]) {
                            worklist.add(t);
                            inQueue[t] = true;
                        }
                    }
                }
                if (idx + 1 < n && mergeInto(inStates, idx + 1, out) && !inQueue[idx + 1]) {
                    worklist.add(idx + 1);
                    inQueue[idx + 1] = true;
                }
            } else if (isTerminator(stmt)) {
                // Không fall-through
            } else if (idx + 1 < n) {
                if (mergeInto(inStates, idx + 1, out) && !inQueue[idx + 1]) {
                    worklist.add(idx + 1);
                    inQueue[idx + 1] = true;
                }
            }
        }

        // Thu thập danh sách lệnh invoke-direct <init> vi phạm quy tắc JVM
        List<RewriteTarget> targets = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<Integer, UninitRef> state = inStates[i];
            if (state == null || state.isEmpty()) continue;
            DexStmtNode stmt = stmts.get(i);
            if (!(stmt instanceof MethodStmtNode ms)) continue;
            if (ms.op != Op.INVOKE_DIRECT && ms.op != Op.INVOKE_DIRECT_RANGE) continue;
            if (ms.method == null || !"<init>".equals(ms.method.getName())) continue;
            if (ms.args == null || ms.args.length == 0) continue;

            UninitRef ref = state.get(ms.args[0]);
            if (ref == null) continue;

            String calledOwner = ms.method.getOwner();
            if (!ref.isThis()) {
                // Đối tượng tạo từ NEW_INSTANCE Sub -> bắt buộc gọi Sub.<init>
                if (!ref.typeDesc().equals(calledOwner)) {
                    targets.add(new RewriteTarget(i, ms, ref.typeDesc()));
                }
            } else {
                // Bên trong C.<init>, this chỉ được gọi C.<init> hoặc C.superClass.<init>
                if (!cls.className.equals(calledOwner)
                        && cls.superClass != null
                        && !cls.superClass.equals(calledOwner)) {
                    targets.add(new RewriteTarget(i, ms, cls.superClass));
                }
            }
        }
        return targets;
    }

    private static Map<Integer, UninitRef> transfer(DexStmtNode stmt, int idx, Map<Integer, UninitRef> in) {
        if (stmt instanceof TypeStmtNode ts) {
            if (ts.op == Op.NEW_INSTANCE) {
                Map<Integer, UninitRef> out = new HashMap<>(in);
                out.put(ts.a, new UninitRef(ts.type, idx, false));
                return out;
            }
            if (ts.op == Op.NEW_ARRAY || ts.op == Op.INSTANCE_OF) {
                return removeReg(in, ts.a, false);
            }
            return in;
        }
        if (stmt instanceof Stmt2RNode s2) {
            if (s2.op == Op.MOVE_OBJECT || s2.op == Op.MOVE_OBJECT_FROM16 || s2.op == Op.MOVE_OBJECT_16) {
                UninitRef src = in.get(s2.b);
                if (src == null && !in.containsKey(s2.a)) return in;
                Map<Integer, UninitRef> out = new HashMap<>(in);
                if (src != null) out.put(s2.a, src);
                else out.remove(s2.a);
                return out;
            }
            return removeReg(in, s2.a, isWideDest(s2.op));
        }
        if (stmt instanceof MethodStmtNode ms) {
            if ((ms.op == Op.INVOKE_DIRECT || ms.op == Op.INVOKE_DIRECT_RANGE)
                    && ms.method != null && "<init>".equals(ms.method.getName())
                    && ms.args != null && ms.args.length > 0) {
                UninitRef target = in.get(ms.args[0]);
                if (target != null) {
                    Map<Integer, UninitRef> out = new HashMap<>(in);
                    out.entrySet().removeIf(e -> e.getValue().allocSiteIndex() == target.allocSiteIndex()
                            && e.getValue().isThis() == target.isThis());
                    return out;
                }
            }
            return in;
        }
        if (stmt instanceof ConstStmtNode cs) {
            return removeReg(in, cs.a, isWideDest(cs.op));
        }
        if (stmt instanceof FieldStmtNode fs) {
            String opName = fs.op != null ? fs.op.name() : "";
            if (opName.startsWith("IGET") || opName.startsWith("SGET")) {
                return removeReg(in, fs.a, opName.contains("WIDE"));
            }
            return in;
        }
        if (stmt instanceof Stmt1RNode s1) {
            if (s1.op == Op.MOVE_RESULT || s1.op == Op.MOVE_RESULT_WIDE
                    || s1.op == Op.MOVE_RESULT_OBJECT || s1.op == Op.MOVE_EXCEPTION) {
                return removeReg(in, s1.a, s1.op == Op.MOVE_RESULT_WIDE);
            }
            return in;
        }
        if (stmt instanceof Stmt2R1NNode s2n) {
            return removeReg(in, s2n.distReg, false);
        }
        if (stmt instanceof Stmt3RNode s3) {
            String opName = s3.op != null ? s3.op.name() : "";
            if (!opName.startsWith("APUT")) {
                return removeReg(in, s3.a, opName.contains("WIDE") || opName.contains("LONG") || opName.contains("DOUBLE"));
            }
            return in;
        }
        return in;
    }

    private static Map<Integer, UninitRef> removeReg(Map<Integer, UninitRef> in, int reg, boolean wide) {
        if (!in.containsKey(reg) && (!wide || !in.containsKey(reg + 1))) {
            return in;
        }
        Map<Integer, UninitRef> out = new HashMap<>(in);
        out.remove(reg);
        if (wide) {
            out.remove(reg + 1);
        }
        return out;
    }

    private static boolean isWideDest(Op op) {
        if (op == null) return false;
        String n = op.name();
        return n.contains("WIDE") || n.endsWith("_LONG") || n.endsWith("_DOUBLE");
    }

    private static boolean isTerminator(DexStmtNode stmt) {
        if (stmt == null || stmt.op == null) return false;
        return switch (stmt.op) {
            case RETURN_VOID, RETURN, RETURN_WIDE, RETURN_OBJECT, THROW -> true;
            default -> false;
        };
    }

    private static boolean mergeInto(Map<Integer, UninitRef>[] inStates, int targetIdx, Map<Integer, UninitRef> incoming) {
        Map<Integer, UninitRef> existing = inStates[targetIdx];
        if (existing == null) {
            inStates[targetIdx] = new HashMap<>(incoming);
            return true;
        }
        boolean changed = false;
        var it = existing.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            UninitRef inc = incoming.get(e.getKey());
            if (inc == null || !Objects.equals(inc.typeDesc(), e.getValue().typeDesc())
                    || inc.isThis() != e.getValue().isThis()) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static int applyRewrites(
            DexCodeNode code,
            List<RewriteTarget> targets,
            Map<String, DexClassNode> classMap,
            Map<String, Set<String>> existingCtors,
            Map<BridgeKey, Method> bridgeCache,
            int[] synthCount) {
        record ResolvedRewrite(int stmtIndex, MethodStmtNode origStmt, Method resolvedCtor, int extraMarkers) {
        }

        List<ResolvedRewrite> resolved = new ArrayList<>(targets.size());
        boolean needsNullRegister = false;
        for (RewriteTarget t : targets) {
            Method origMethod = t.origStmt().method;
            Method bridge = ensureBridgeConstructor(
                    t.requiredOwner(),
                    origMethod.getOwner(),
                    origMethod.getParameterTypes(),
                    classMap,
                    existingCtors,
                    bridgeCache,
                    synthCount);
            if (bridge == null) continue;
            int extraMarkers = bridge.getParameterTypes().length - origMethod.getParameterTypes().length;
            if (extraMarkers > 0) {
                needsNullRegister = true;
            }
            resolved.add(new ResolvedRewrite(t.stmtIndex(), t.origStmt(), bridge, extraMarkers));
        }

        if (resolved.isEmpty()) {
            return 0;
        }

        if (needsNullRegister) {
            shiftAllRegisters(code, 1);
            // Đánh dấu chỉ số đã dịch nếu cần chèn CONST null ở vị trí 0 (chèn CONST vào đầu hàm hoặc ngay trước lệnh gọi)
            // Chèn lệnh `const/4 v0, 0` ngay trước mỗi điểm gọi cần marker theo thứ tự từ cuối lên đầu
            // để không ảnh hưởng chỉ số của các lệnh trước đó và đảm bảo v0 luôn có giá trị null kể cả qua label/nhánh.
            resolved.sort((a, b) -> Integer.compare(b.stmtIndex(), a.stmtIndex()));
            for (ResolvedRewrite rw : resolved) {
                MethodStmtNode shiftedMs = (MethodStmtNode) code.stmts.get(rw.stmtIndex());
                if (rw.extraMarkers() == 0) {
                    code.stmts.set(rw.stmtIndex(), new MethodStmtNode(shiftedMs.op, shiftedMs.args, rw.resolvedCtor()));
                } else {
                    int[] newArgs = Arrays.copyOf(shiftedMs.args, shiftedMs.args.length + rw.extraMarkers());
                    for (int k = shiftedMs.args.length; k < newArgs.length; k++) {
                        newArgs[k] = 0; // reg 0 đã dành riêng cho null marker
                    }
                    code.stmts.set(rw.stmtIndex(), new MethodStmtNode(Op.INVOKE_DIRECT, newArgs, rw.resolvedCtor()));
                    code.stmts.add(rw.stmtIndex(), new ConstStmtNode(Op.CONST_4, 0, 0));
                }
            }
        } else {
            for (ResolvedRewrite rw : resolved) {
                MethodStmtNode ms = (MethodStmtNode) code.stmts.get(rw.stmtIndex());
                code.stmts.set(rw.stmtIndex(), new MethodStmtNode(ms.op, ms.args, rw.resolvedCtor()));
            }
        }
        return resolved.size();
    }

    /**
     * Tổng hợp chuỗi bridge constructor từ {@code startClassDesc} lên tới {@code targetOwnerDesc}.
     */
    private static Method ensureBridgeConstructor(
            String startClassDesc,
            String targetOwnerDesc,
            String[] origParamTypes,
            Map<String, DexClassNode> classMap,
            Map<String, Set<String>> existingCtors,
            Map<BridgeKey, Method> bridgeCache,
            int[] synthCount) {
        List<String> chain = new ArrayList<>();
        String cur = startClassDesc;
        for (int depth = 0; depth < 32 && cur != null; depth++) {
            if (cur.equals(targetOwnerDesc)) {
                break;
            }
            DexClassNode node = classMap.get(cur);
            if (node == null) {
                // Gặp lớp trung gian nằm ngoài APK -> không thể tổng hợp bridge trên lớp ngoài
                return null;
            }
            chain.add(cur);
            cur = node.superClass;
        }
        if (chain.isEmpty() || !Objects.equals(cur, targetOwnerDesc)) {
            return null;
        }

        Method superTarget = new Method(targetOwnerDesc, "<init>", origParamTypes, "V");
        for (int i = chain.size() - 1; i >= 0; i--) {
            String clsDesc = chain.get(i);
            BridgeKey key = new BridgeKey(clsDesc, superTarget.getOwner(), superTarget.getDesc());
            Method cached = bridgeCache.get(key);
            if (cached != null) {
                superTarget = cached;
                continue;
            }

            DexClassNode clsNode = classMap.get(clsDesc);
            Set<String> ctorSet = existingCtors.computeIfAbsent(clsDesc, k -> new HashSet<>());

            // Ưu tiên giữ nguyên chữ ký origParamTypes nếu lớp chưa có constructor trùng chữ ký
            String[] bridgeParams = origParamTypes;
            Method candidate = new Method(clsDesc, "<init>", bridgeParams, "V");
            while (ctorSet.contains(candidate.getDesc())) {
                bridgeParams = appendParam(bridgeParams, MARKER_DESC);
                candidate = new Method(clsDesc, "<init>", bridgeParams, "V");
            }

            synthesizeBridgeConstructor(clsNode, candidate, origParamTypes, superTarget);
            ctorSet.add(candidate.getDesc());
            bridgeCache.put(key, candidate);
            synthCount[0]++;
            superTarget = candidate;
        }
        return superTarget;
    }

    private static void synthesizeBridgeConstructor(
            DexClassNode clsNode,
            Method bridgeMethod,
            String[] origParamTypes,
            Method superTarget) {
        int bridgeParamSlots = computeParamSlots(bridgeMethod.getParameterTypes(), false);
        int origParamSlots = computeParamSlots(origParamTypes, false);
        int superExtraMarkers = superTarget.getParameterTypes().length - origParamTypes.length;
        int extraLocals = superExtraMarkers > 0 ? 1 : 0;

        DexCodeNode code = new DexCodeNode();
        code.totalRegister = extraLocals + bridgeParamSlots;
        code.stmts = new ArrayList<>();

        int paramBase = extraLocals; // Quy ước DEX: locals = [0..extraLocals-1], params = [extraLocals..total-1]
        if (extraLocals > 0) {
            code.stmts.add(new ConstStmtNode(Op.CONST_4, 0, 0));
        }

        int superArgSlots = origParamSlots + superExtraMarkers;
        int[] superCallArgs = new int[superArgSlots];
        for (int i = 0; i < origParamSlots; i++) {
            superCallArgs[i] = paramBase + i;
        }
        for (int i = origParamSlots; i < superArgSlots; i++) {
            superCallArgs[i] = 0; // null marker tại reg 0
        }

        code.stmts.add(new MethodStmtNode(Op.INVOKE_DIRECT, superCallArgs, superTarget));
        code.stmts.add(new Stmt0RNode(Op.RETURN_VOID));

        DexMethodNode mn = new DexMethodNode(
                DexConstants.ACC_PUBLIC | DexConstants.ACC_CONSTRUCTOR | DexConstants.ACC_SYNTHETIC,
                bridgeMethod);
        mn.codeNode = code;
        if (clsNode.methods == null) {
            clsNode.methods = new ArrayList<>();
        }
        clsNode.methods.add(mn);
    }

    private static void shiftAllRegisters(DexCodeNode code, int delta) {
        code.totalRegister += delta;
        code.debugNode = null; // Tránh lệch bảng tên biến cục bộ debug khi dịch thanh ghi
        List<DexStmtNode> stmts = code.stmts;
        for (int i = 0; i < stmts.size(); i++) {
            DexStmtNode s = stmts.get(i);
            if (s instanceof ConstStmtNode cs) {
                stmts.set(i, new ConstStmtNode(cs.op, cs.a + delta, cs.value));
            } else if (s instanceof FieldStmtNode fs) {
                boolean hasObjReg = fs.op != null && (fs.op.name().startsWith("IGET") || fs.op.name().startsWith("IPUT"));
                stmts.set(i, new FieldStmtNode(fs.op, fs.a + delta, hasObjReg ? fs.b + delta : fs.b, fs.field));
            } else if (s instanceof FillArrayDataStmtNode fad) {
                stmts.set(i, new FillArrayDataStmtNode(fad.op, fad.ra + delta, fad.array));
            } else if (s instanceof FilledNewArrayStmtNode fna) {
                stmts.set(i, new FilledNewArrayStmtNode(fna.op, shiftArray(fna.args, delta), fna.type));
            } else if (s instanceof JumpStmtNode js) {
                boolean isGoto = js.op == Op.GOTO || js.op == Op.GOTO_16 || js.op == Op.GOTO_32;
                boolean hasB = !isGoto && js.op != null && !js.op.name().endsWith("Z");
                stmts.set(i, new JumpStmtNode(js.op, isGoto ? js.a : js.a + delta, hasB ? js.b + delta : js.b, js.label));
            } else if (s instanceof MethodStmtNode ms) {
                stmts.set(i, new MethodStmtNode(ms.op, shiftArray(ms.args, delta), ms.method));
            } else if (s instanceof MethodCustomStmtNode mc) {
                stmts.set(i, new MethodCustomStmtNode(mc.op, shiftArray(mc.args, delta), mc.callSite));
            } else if (s instanceof MethodPolymorphicStmtNode mp) {
                stmts.set(i, new MethodPolymorphicStmtNode(mp.op, shiftArray(mp.args, delta), mp.method, mp.proto));
            } else if (s instanceof PackedSwitchStmtNode ps) {
                stmts.set(i, new PackedSwitchStmtNode(ps.op, ps.a + delta, ps.firstCase, ps.labels));
            } else if (s instanceof SparseSwitchStmtNode ss) {
                stmts.set(i, new SparseSwitchStmtNode(ss.op, ss.a + delta, ss.cases, ss.labels));
            } else if (s instanceof Stmt1RNode s1) {
                stmts.set(i, new Stmt1RNode(s1.op, s1.a + delta));
            } else if (s instanceof Stmt2RNode s2) {
                stmts.set(i, new Stmt2RNode(s2.op, s2.a + delta, s2.b + delta));
            } else if (s instanceof Stmt2R1NNode s2n) {
                stmts.set(i, new Stmt2R1NNode(s2n.op, s2n.distReg + delta, s2n.srcReg + delta, s2n.content));
            } else if (s instanceof Stmt3RNode s3) {
                stmts.set(i, new Stmt3RNode(s3.op, s3.a + delta, s3.b + delta, s3.c + delta));
            } else if (s instanceof TypeStmtNode ts) {
                boolean hasB = ts.op == Op.INSTANCE_OF || ts.op == Op.NEW_ARRAY;
                stmts.set(i, new TypeStmtNode(ts.op, ts.a + delta, hasB ? ts.b + delta : ts.b, ts.type));
            }
        }
    }

    private static int[] shiftArray(int[] arr, int delta) {
        if (arr == null) return null;
        int[] out = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            out[i] = arr[i] + delta;
        }
        return out;
    }

    private static String[] appendParam(String[] orig, String extra) {
        String[] out = Arrays.copyOf(orig, orig.length + 1);
        out[orig.length] = extra;
        return out;
    }

    private static int computeParamSlots(String[] paramTypes, boolean isStatic) {
        int slots = isStatic ? 0 : 1;
        if (paramTypes != null) {
            for (String p : paramTypes) {
                if (p != null && !p.isEmpty() && (p.charAt(0) == 'J' || p.charAt(0) == 'D')) {
                    slots += 2;
                } else {
                    slots += 1;
                }
            }
        }
        return slots;
    }

    private static final class DexFileNodeReader implements BaseDexFileReader {
        private final DexFileNode fileNode;
        private final int dexVersion;

        DexFileNodeReader(DexFileNode fileNode, int dexVersion) {
            this.fileNode = fileNode;
            this.dexVersion = dexVersion;
        }

        @Override
        public int getDexVersion() {
            return dexVersion;
        }

        @Override
        public List<String> getClassNames() {
            if (fileNode.clzs == null) return Collections.emptyList();
            List<String> names = new ArrayList<>(fileNode.clzs.size());
            for (DexClassNode c : fileNode.clzs) {
                if (c != null && c.className != null) {
                    names.add(c.className);
                }
            }
            return names;
        }

        @Override
        public void accept(DexFileVisitor dv) {
            acceptAndRelease(dv);
        }

        @Override
        public void accept(DexFileVisitor dv, int config) {
            acceptAndRelease(dv);
        }

        private void acceptAndRelease(DexFileVisitor dv) {
            if (fileNode.clzs == null) return;
            dv.visitDexFileVersion(dexVersion);
            for (int i = 0; i < fileNode.clzs.size(); i++) {
                DexClassNode cls = fileNode.clzs.get(i);
                if (cls != null) {
                    cls.accept(dv);
                    // Giải phóng AST DEX ngay sau khi dex2jar tiêu thụ xong từng class để giảm đỉnh RAM
                    fileNode.clzs.set(i, null);
                }
            }
            dv.visitEnd();
        }

        @Override
        public void accept(DexFileVisitor dv, int classIdx, int config) {
            if (fileNode.clzs != null && classIdx >= 0 && classIdx < fileNode.clzs.size()) {
                fileNode.clzs.get(classIdx).accept(dv);
            }
        }
    }
}
