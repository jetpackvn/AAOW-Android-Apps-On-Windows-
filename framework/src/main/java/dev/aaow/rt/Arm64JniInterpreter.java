package dev.aaow.rt;

import dev.aaow.apk.ElfFile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Trình thông dịch CPU ARM64 (AArch64) thuần Java mức người dùng cho các hàm tính toán JNI nhỏ gọn
 * trích xuất từ phân đoạn PT_LOAD của file ELF (.so) arm64-v8a.
 * Nếu hàm gọi tới syscall OS hoặc ký hiệu PLT bên ngoài chưa giả lập, tự động trả về trạng thái unhandled
 * để NativeBridge chuyển sang lớp giả lập Java HLE hoặc giá trị mặc định an toàn.
 */
public final class Arm64JniInterpreter {
    public static final long CODE_BASE_ADDR = 0x0010_0000L;
    private static final long JNI_ENV_PTR = 0x7D00_0000L;
    private static final long JNI_VTABLE_ADDR = 0x7D00_0100L;
    private static final long JNI_TRAP_BASE = 0x7D10_0000L;
    private static final long HEAP_BASE = 0x7E00_0000L;
    private static final int HEAP_SIZE = 256 * 1024;
    private static final long STACK_BASE = 0x7F00_0000L;
    private static final int STACK_SIZE = 256 * 1024;
    private static final long RETURN_SENTINEL = 0xDEAD_BEEF_0000_0000L;
    private static final int MAX_STEPS = 200_000;

    public record ExecutionResult(boolean handled, Object returnValue, int instructionsExecuted, String reason) {
        public static ExecutionResult ok(Object value, int steps) {
            return new ExecutionResult(true, value, steps, null);
        }

        public static ExecutionResult unhandled(String reason, int steps) {
            return new ExecutionResult(false, null, steps, reason);
        }
    }

    private Arm64JniInterpreter() {
    }

    public static ExecutionResult executeJni(ElfFile.VirtualImage image, ElfFile.ElfSymbol symbol,
                                             String javaMethodDescriptor, Object[] javaArgs) {
        if (image == null || symbol == null || image.size() == 0) {
            return ExecutionResult.unhandled("Ảnh bộ nhớ ELF rỗng", 0);
        }

        VmState vm = new VmState(image);
        vm.setupJniEnv();
        vm.bindJniArgs(javaMethodDescriptor, javaArgs);

        long entryPc = image.mapAddress(symbol.address());
        vm.pc = entryPc;
        vm.regs[30] = RETURN_SENTINEL; // LR
        vm.sp = STACK_BASE + STACK_SIZE - 0x1000L;

        int steps = 0;
        while (steps < MAX_STEPS) {
            if (vm.pc == RETURN_SENTINEL) {
                Object ret = vm.extractReturnValue(javaMethodDescriptor);
                return ExecutionResult.ok(ret, steps);
            }
            if (vm.pc >= JNI_TRAP_BASE && vm.pc < JNI_TRAP_BASE + 0x2000L) {
                int jniIndex = (int) ((vm.pc - JNI_TRAP_BASE) / 4L);
                if (!vm.handleJniTrap(jniIndex)) {
                    return ExecutionResult.unhandled("Hàm JNIEnv index=" + jniIndex + " chưa hỗ trợ", steps);
                }
                vm.pc = vm.regs[30];
                continue;
            }
            if (!vm.isExecutableCodeAddr(vm.pc)) {
                return ExecutionResult.unhandled("PC nhảy ra ngoài vùng mã thực thi: 0x" + Long.toHexString(vm.pc), steps);
            }

            int insn = vm.readInt32(vm.pc);
            steps++;
            long nextPc = vm.pc + 4L;
            String err = vm.step(insn, nextPc);
            if (err != null) {
                return ExecutionResult.unhandled(err, steps);
            }
        }
        return ExecutionResult.unhandled("Vượt quá giới hạn " + MAX_STEPS + " lệnh ARM64", steps);
    }

    private static final class VmState {
        final ElfFile.VirtualImage image;
        final byte[] codeMem;
        final long codeStart;
        final long codeEnd;
        final byte[] stackMem = new byte[STACK_SIZE];
        final byte[] heapMem = new byte[HEAP_SIZE];
        final byte[] jniMem = new byte[8192];
        int heapPtr = 64;

        final long[] regs = new long[31]; // X0..X30
        long sp;
        long pc;
        boolean flagN, flagZ, flagC, flagV;

        final Map<Long, Object> jniHandles = new HashMap<>();
        long nextHandle = 0x1000L;

        VmState(ElfFile.VirtualImage image) {
            this.image = image;
            this.codeMem = image.getMemory();
            this.codeStart = image.getBaseAddress();
            this.codeEnd = image.getBaseAddress() + codeMem.length;
        }

        void setupJniEnv() {
            // *JNIEnv = JNI_VTABLE_ADDR
            writeLong(JNI_ENV_PTR, JNI_VTABLE_ADDR);
            for (int i = 0; i < 300; i++) {
                writeLong(JNI_VTABLE_ADDR + i * 8L, JNI_TRAP_BASE + i * 4L);
            }
        }

        long allocHandle(Object obj) {
            if (obj == null) return 0L;
            long h = nextHandle++;
            jniHandles.put(h, obj);
            return h;
        }

        void bindJniArgs(String descriptor, Object[] javaArgs) {
            regs[0] = JNI_ENV_PTR; // JNIEnv*
            regs[1] = 0x200L;      // jobject / jclass
            if (javaArgs == null) return;

            String[] argTypes = parseParamDescriptors(descriptor);
            int regIdx = 2;
            for (int i = 0; i < javaArgs.length && i < argTypes.length && regIdx < 8; i++) {
                String t = argTypes[i];
                Object val = javaArgs[i];
                if ("I".equals(t) || "S".equals(t) || "B".equals(t) || "C".equals(t) || "Z".equals(t)) {
                    if (val instanceof Boolean b) regs[regIdx] = b ? 1L : 0L;
                    else if (val instanceof Character c) regs[regIdx] = c;
                    else if (val instanceof Number n) regs[regIdx] = n.longValue();
                } else if ("J".equals(t)) {
                    regs[regIdx] = val instanceof Number n ? n.longValue() : 0L;
                } else {
                    regs[regIdx] = allocHandle(val);
                }
                regIdx++;
            }
        }

        Object extractReturnValue(String descriptor) {
            int close = descriptor.indexOf(')');
            String ret = close >= 0 ? descriptor.substring(close + 1) : "V";
            return switch (ret) {
                case "V" -> null;
                case "Z" -> (regs[0] & 1L) != 0;
                case "B" -> (byte) regs[0];
                case "C" -> (char) regs[0];
                case "S" -> (short) regs[0];
                case "I" -> (int) regs[0];
                case "J" -> regs[0];
                case "F" -> Float.intBitsToFloat((int) regs[0]);
                case "D" -> Double.longBitsToDouble(regs[0]);
                default -> regs[0] == 0L ? null : jniHandles.get(regs[0]);
            };
        }

        boolean handleJniTrap(int index) {
            switch (index) {
                case 4 -> { // GetVersion
                    regs[0] = 0x00010006L; // JNI_VERSION_1_6
                    return true;
                }
                case 167 -> { // NewStringUTF(env, char*)
                    String s = readCString(regs[1]);
                    regs[0] = allocHandle(s);
                    return true;
                }
                case 168 -> { // GetStringUTFLength(env, jstring)
                    Object obj = jniHandles.get(regs[1]);
                    regs[0] = obj instanceof String s ? s.getBytes(StandardCharsets.UTF_8).length : 0L;
                    return true;
                }
                case 169 -> { // GetStringUTFChars(env, jstring, isCopy*)
                    Object obj = jniHandles.get(regs[1]);
                    String s = obj instanceof String str ? str : "";
                    if (regs[2] != 0) writeByte(regs[2], (byte) 1);
                    byte[] utf = s.getBytes(StandardCharsets.UTF_8);
                    long ptr = allocHeap(utf.length + 1);
                    for (int i = 0; i < utf.length; i++) writeByte(ptr + i, utf[i]);
                    writeByte(ptr + utf.length, (byte) 0);
                    regs[0] = ptr;
                    return true;
                }
                case 170 -> { // ReleaseStringUTFChars
                    return true;
                }
                case 171 -> { // GetArrayLength(env, jarray)
                    Object arr = jniHandles.get(regs[1]);
                    regs[0] = arr != null && arr.getClass().isArray() ? java.lang.reflect.Array.getLength(arr) : 0L;
                    return true;
                }
                case 173 -> { // NewByteArray(env, len)
                    int len = Math.max(0, (int) regs[1]);
                    regs[0] = allocHandle(new byte[len]);
                    return true;
                }
                case 176 -> { // NewIntArray(env, len)
                    int len = Math.max(0, (int) regs[1]);
                    regs[0] = allocHandle(new int[len]);
                    return true;
                }
                case 184 -> { // GetByteArrayElements
                    Object arr = jniHandles.get(regs[1]);
                    if (regs[2] != 0) writeByte(regs[2], (byte) 1);
                    if (arr instanceof byte[] b) {
                        long ptr = allocHeap(Math.max(1, b.length));
                        for (int i = 0; i < b.length; i++) writeByte(ptr + i, b[i]);
                        regs[0] = ptr;
                        return true;
                    }
                    regs[0] = 0L;
                    return true;
                }
                case 187 -> { // GetIntArrayElements
                    Object arr = jniHandles.get(regs[1]);
                    if (regs[2] != 0) writeByte(regs[2], (byte) 1);
                    if (arr instanceof int[] a) {
                        long ptr = allocHeap(Math.max(4, a.length * 4));
                        for (int i = 0; i < a.length; i++) writeInt32(ptr + i * 4L, a[i]);
                        regs[0] = ptr;
                        return true;
                    }
                    regs[0] = 0L;
                    return true;
                }
                case 192, 195 -> { // ReleaseByteArrayElements / ReleaseIntArrayElements
                    return true;
                }
                case 200 -> { // GetByteArrayRegion(env, arr, start, len, buf)
                    if (jniHandles.get(regs[1]) instanceof byte[] b) {
                        int start = (int) regs[2];
                        int len = (int) regs[3];
                        long dst = regs[4];
                        for (int i = 0; i < len && start + i < b.length; i++) {
                            writeByte(dst + i, b[start + i]);
                        }
                    }
                    return true;
                }
                case 203 -> { // GetIntArrayRegion(env, arr, start, len, buf)
                    if (jniHandles.get(regs[1]) instanceof int[] a) {
                        int start = (int) regs[2];
                        int len = (int) regs[3];
                        long dst = regs[4];
                        for (int i = 0; i < len && start + i < a.length; i++) {
                            writeInt32(dst + i * 4L, a[start + i]);
                        }
                    }
                    return true;
                }
                case 208 -> { // SetByteArrayRegion(env, arr, start, len, buf)
                    if (jniHandles.get(regs[1]) instanceof byte[] b) {
                        int start = (int) regs[2];
                        int len = (int) regs[3];
                        long src = regs[4];
                        for (int i = 0; i < len && start + i < b.length; i++) {
                            b[start + i] = readByte(src + i);
                        }
                    }
                    return true;
                }
                case 211 -> { // SetIntArrayRegion(env, arr, start, len, buf)
                    if (jniHandles.get(regs[1]) instanceof int[] a) {
                        int start = (int) regs[2];
                        int len = (int) regs[3];
                        long src = regs[4];
                        for (int i = 0; i < len && start + i < a.length; i++) {
                            a[start + i] = readInt32(src + i * 4L);
                        }
                    }
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }

        long allocHeap(int size) {
            int aligned = (size + 15) & ~15;
            if (heapPtr + aligned >= HEAP_SIZE) return HEAP_BASE;
            long addr = HEAP_BASE + heapPtr;
            heapPtr += aligned;
            return addr;
        }

        boolean isExecutableCodeAddr(long addr) {
            return addr >= codeStart && addr + 4L <= codeEnd;
        }

        String step(int insn, long defaultNextPc) {
            // NOP & HINT
            if (insn == 0xD503201F || (insn & 0xFFFFF000) == 0xD5032000 || (insn & 0xFFFFF000) == 0xD5033000) {
                pc = defaultNextPc;
                return null;
            }

            // Unconditional branch immediate: B / BL
            if ((insn & 0x7C000000) == 0x14000000) {
                boolean link = (insn & 0x80000000) != 0;
                long imm26 = signExtend(insn & 0x03FFFFFF, 26) << 2;
                if (link) regs[30] = defaultNextPc;
                pc += imm26;
                return null;
            }

            // Conditional branch: B.cond
            if ((insn & 0xFF000010) == 0x54000000) {
                int cond = insn & 0xF;
                long imm19 = signExtend((insn >>> 5) & 0x7FFFF, 19) << 2;
                pc = evalCond(cond) ? pc + imm19 : defaultNextPc;
                return null;
            }

            // CBZ / CBNZ
            if ((insn & 0x7E000000) == 0x34000000) {
                boolean sf = (insn >>> 31) != 0;
                boolean nz = ((insn >>> 24) & 1) != 0;
                int rt = insn & 0x1F;
                long imm19 = signExtend((insn >>> 5) & 0x7FFFF, 19) << 2;
                long v = getX(rt, false);
                if (!sf) v &= 0xFFFFFFFFL;
                boolean take = nz ? (v != 0) : (v == 0);
                pc = take ? pc + imm19 : defaultNextPc;
                return null;
            }

            // TBZ / TBNZ
            if ((insn & 0x7E000000) == 0x36000000) {
                boolean nz = ((insn >>> 24) & 1) != 0;
                int bit = (((insn >>> 31) & 1) << 5) | ((insn >>> 19) & 0x1F);
                int rt = insn & 0x1F;
                long imm14 = signExtend((insn >>> 5) & 0x3FFF, 14) << 2;
                boolean isSet = ((getX(rt, false) >>> bit) & 1L) != 0L;
                pc = (nz ? isSet : !isSet) ? pc + imm14 : defaultNextPc;
                return null;
            }

            // BR / BLR / RET
            if ((insn & 0xFE9FFC1F) == 0xD61F0000) {
                int opc = (insn >>> 21) & 0xF;
                int rn = (insn >>> 5) & 0x1F;
                long target = getX(rn, false);
                if (opc == 1) { // BLR
                    regs[30] = defaultNextPc;
                    pc = target;
                    return null;
                } else if (opc == 0 || opc == 2) { // BR / RET
                    pc = target;
                    return null;
                }
            }

            // ADR / ADRP
            if ((insn & 0x1F000000) == 0x10000000) {
                boolean isAdrp = (insn >>> 31) != 0;
                int rd = insn & 0x1F;
                long imm = (((long) ((insn >>> 5) & 0x7FFFF)) << 2) | ((insn >>> 29) & 3);
                imm = signExtend(imm, 21);
                long res = isAdrp ? ((pc & ~0xFFFL) + (imm << 12)) : (pc + imm);
                setX(rd, res, false, true);
                pc = defaultNextPc;
                return null;
            }

            // Move wide (MOVN / MOVZ / MOVK)
            if ((insn & 0x1F800000) == 0x12800000) {
                boolean sf = (insn >>> 31) != 0;
                int opc = (insn >>> 29) & 3;
                int hw = (insn >>> 21) & 3;
                long imm16 = (insn >>> 5) & 0xFFFFL;
                int rd = insn & 0x1F;
                int shift = hw * 16;
                long res;
                if (opc == 2) { // MOVZ
                    res = imm16 << shift;
                } else if (opc == 0) { // MOVN
                    res = ~(imm16 << shift);
                } else if (opc == 3) { // MOVK
                    long old = getX(rd, false);
                    res = (old & ~(0xFFFFL << shift)) | (imm16 << shift);
                } else {
                    return "MOV opcode không hợp lệ: " + opc;
                }
                setX(rd, res, false, sf);
                pc = defaultNextPc;
                return null;
            }

            // ADD / ADDS / SUB / SUBS (immediate)
            if ((insn & 0x1F800000) == 0x11000000) {
                boolean sf = (insn >>> 31) != 0;
                boolean sub = ((insn >>> 30) & 1) != 0;
                boolean setFlags = ((insn >>> 29) & 1) != 0;
                int sh = (insn >>> 22) & 3;
                long imm12 = (insn >>> 10) & 0xFFFL;
                if (sh == 1) imm12 <<= 12;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                long lhs = getX(rn, true);
                long res = execAddSub(lhs, imm12, sub, setFlags, sf);
                setX(rd, res, !setFlags, sf);
                pc = defaultNextPc;
                return null;
            }

            // Bitfield: SBFM / BFM / UBFM
            if ((insn & 0x1F800000) == 0x13000000) {
                boolean sf = (insn >>> 31) != 0;
                int opc = (insn >>> 29) & 3;
                int immr = (insn >>> 16) & 0x3F;
                int imms = (insn >>> 10) & 0x3F;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                int width = sf ? 64 : 32;
                long src = getX(rn, false);
                if (!sf) src &= 0xFFFFFFFFL;
                long res;
                if (opc == 2 || opc == 0) { // UBFM (2) or SBFM (0)
                    boolean signed = (opc == 0);
                    if (imms >= immr) {
                        int len = imms - immr + 1;
                        long mask = len >= 64 ? -1L : ((1L << len) - 1L);
                        res = (src >>> immr) & mask;
                        if (signed && len < 64 && ((res >>> (len - 1)) & 1L) != 0L) {
                            res |= ~mask;
                        }
                    } else {
                        int shift = width - immr;
                        int len = imms + 1;
                        long mask = len >= 64 ? -1L : ((1L << len) - 1L);
                        res = (src & mask) << shift;
                        if (signed && len < 64 && ((src >>> (len - 1)) & 1L) != 0L) {
                            res |= (~0L) << (shift + len);
                        }
                    }
                    setX(rd, res, false, sf);
                    pc = defaultNextPc;
                    return null;
                }
            }

            // Logical (shifted register): AND / ORR / EOR / ANDS
            if ((insn & 0x1F000000) == 0x0A000000) {
                boolean sf = (insn >>> 31) != 0;
                int opc = (insn >>> 29) & 3;
                int shiftType = (insn >>> 22) & 3;
                boolean invert = ((insn >>> 21) & 1) != 0;
                int rm = (insn >>> 16) & 0x1F;
                int imm6 = (insn >>> 10) & 0x3F;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;

                long op1 = getX(rn, false);
                long op2 = shiftReg(getX(rm, false), shiftType, imm6, sf);
                if (invert) op2 = ~op2;
                long res = switch (opc) {
                    case 0, 3 -> op1 & op2;
                    case 1 -> op1 | op2;
                    default -> op1 ^ op2;
                };
                if (opc == 3) { // ANDS / TST
                    updateLogicFlags(res, sf);
                }
                setX(rd, res, false, sf);
                pc = defaultNextPc;
                return null;
            }

            // ADD / ADDS / SUB / SUBS (shifted register)
            if ((insn & 0x1F200000) == 0x0B000000) {
                boolean sf = (insn >>> 31) != 0;
                boolean sub = ((insn >>> 30) & 1) != 0;
                boolean setFlags = ((insn >>> 29) & 1) != 0;
                int shiftType = (insn >>> 22) & 3;
                int rm = (insn >>> 16) & 0x1F;
                int imm6 = (insn >>> 10) & 0x3F;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                long lhs = getX(rn, false);
                long rhs = shiftReg(getX(rm, false), shiftType, imm6, sf);
                long res = execAddSub(lhs, rhs, sub, setFlags, sf);
                setX(rd, res, false, sf);
                pc = defaultNextPc;
                return null;
            }

            // Data processing (2-source): UDIV / SDIV / LSLV / LSRV / ASRV
            if ((insn & 0x5FE00000) == 0x1AC00000) {
                boolean sf = (insn >>> 31) != 0;
                int rm = (insn >>> 16) & 0x1F;
                int opcode = (insn >>> 10) & 0x3F;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                long a = getX(rn, false);
                long b = getX(rm, false);
                if (!sf) {
                    a = (int) a;
                    b = (int) b;
                }
                long res;
                switch (opcode) {
                    case 2 -> { // UDIV
                        long ua = sf ? a : (a & 0xFFFFFFFFL);
                        long ub = sf ? b : (b & 0xFFFFFFFFL);
                        res = ub == 0 ? 0L : Long.divideUnsigned(ua, ub);
                    }
                    case 3 -> { // SDIV
                        res = b == 0 ? 0L : (a / b);
                    }
                    case 8 -> res = sf ? (a << (b & 63)) : ((a & 0xFFFFFFFFL) << (b & 31));
                    case 9 -> res = sf ? (a >>> (b & 63)) : ((a & 0xFFFFFFFFL) >>> (b & 31));
                    case 10 -> res = sf ? (a >> (b & 63)) : (((int) a) >> (b & 31));
                    default -> {
                        return "Lệnh DP-2src chưa hỗ trợ: " + opcode;
                    }
                }
                setX(rd, res, false, sf);
                pc = defaultNextPc;
                return null;
            }

            // Data processing (3-source): MADD / MSUB / SMADDL / UMADDL
            if ((insn & 0x1F000000) == 0x1B000000) {
                boolean sf = (insn >>> 31) != 0;
                int op31 = (insn >>> 21) & 7;
                int rm = (insn >>> 16) & 0x1F;
                boolean o0 = ((insn >>> 15) & 1) != 0;
                int ra = (insn >>> 10) & 0x1F;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                long n = getX(rn, false);
                long m = getX(rm, false);
                long a = getX(ra, false);
                if (op31 == 0) {
                    long res = o0 ? (a - n * m) : (a + n * m);
                    setX(rd, res, false, sf);
                    pc = defaultNextPc;
                    return null;
                } else if (op31 == 1 || op31 == 5) { // SMADDL / UMADDL
                    long ln = op31 == 1 ? (long) (int) n : (n & 0xFFFFFFFFL);
                    long lm = op31 == 1 ? (long) (int) m : (m & 0xFFFFFFFFL);
                    long res = o0 ? (a - ln * lm) : (a + ln * lm);
                    setX(rd, res, false, true);
                    pc = defaultNextPc;
                    return null;
                }
            }

            // CSEL / CSINC / CSINV / CSNEG
            if ((insn & 0x1FE00800) == 0x1A800000) {
                boolean sf = (insn >>> 31) != 0;
                boolean op = ((insn >>> 30) & 1) != 0;
                int rm = (insn >>> 16) & 0x1F;
                int cond = (insn >>> 12) & 0xF;
                int op2 = (insn >>> 10) & 1;
                int rn = (insn >>> 5) & 0x1F;
                int rd = insn & 0x1F;
                long res;
                if (evalCond(cond)) {
                    res = getX(rn, false);
                } else {
                    long m = getX(rm, false);
                    if (!op && op2 == 1) res = m + 1;
                    else if (op && op2 == 0) res = ~m;
                    else if (op && op2 == 1) res = -m;
                    else res = m;
                }
                setX(rd, res, false, sf);
                pc = defaultNextPc;
                return null;
            }

            // Load/Store Pair: LDP / STP
            if ((insn & 0x3A000000) == 0x28000000) {
                int opc = (insn >>> 30) & 3;
                int mode = (insn >>> 23) & 7;
                boolean load = ((insn >>> 22) & 1) != 0;
                long imm7 = signExtend((insn >>> 15) & 0x7F, 7);
                int rt2 = (insn >>> 10) & 0x1F;
                int rn = (insn >>> 5) & 0x1F;
                int rt = insn & 0x1F;

                int scale = (opc == 2) ? 8 : 4;
                long offset = imm7 * scale;
                long base = getX(rn, true);
                long addr = (mode == 1) ? base : (base + offset);
                if (load) {
                    if (opc == 2) {
                        setX(rt, readLong(addr), false, true);
                        setX(rt2, readLong(addr + 8), false, true);
                    } else if (opc == 1) { // LDPSW
                        setX(rt, readInt32(addr), false, true);
                        setX(rt2, readInt32(addr + 4), false, true);
                    } else {
                        setX(rt, Integer.toUnsignedLong(readInt32(addr)), false, false);
                        setX(rt2, Integer.toUnsignedLong(readInt32(addr + 4)), false, false);
                    }
                } else {
                    if (opc == 2) {
                        writeLong(addr, getX(rt, false));
                        writeLong(addr + 8, getX(rt2, false));
                    } else {
                        writeInt32(addr, (int) getX(rt, false));
                        writeInt32(addr + 4, (int) getX(rt2, false));
                    }
                }
                if (mode == 1 || mode == 3) {
                    setX(rn, base + offset, true, true);
                }
                pc = defaultNextPc;
                return null;
            }

            // Load/Store single register (unsigned imm / pre/post/unscaled / reg offset)
            if ((insn & 0x3B000000) == 0x39000000) { // Unsigned immediate
                int size = (insn >>> 30) & 3;
                int opc = (insn >>> 22) & 3;
                long imm12 = (insn >>> 10) & 0xFFFL;
                int rn = (insn >>> 5) & 0x1F;
                int rt = insn & 0x1F;
                long addr = getX(rn, true) + (imm12 << size);
                execSingleLoadStore(size, opc, rt, addr);
                pc = defaultNextPc;
                return null;
            }

            if ((insn & 0x3B200000) == 0x38000000) { // Unscaled / pre / post index
                int size = (insn >>> 30) & 3;
                int opc = (insn >>> 22) & 3;
                long imm9 = signExtend((insn >>> 12) & 0x1FFL, 9);
                int mode = (insn >>> 10) & 3;
                int rn = (insn >>> 5) & 0x1F;
                int rt = insn & 0x1F;
                long base = getX(rn, true);
                long addr = (mode == 1) ? base : (base + imm9);
                execSingleLoadStore(size, opc, rt, addr);
                if (mode == 1 || mode == 3) {
                    setX(rn, base + imm9, true, true);
                }
                pc = defaultNextPc;
                return null;
            }

            if ((insn & 0x3B200C00) == 0x38200800) { // Register offset
                int size = (insn >>> 30) & 3;
                int opc = (insn >>> 22) & 3;
                int rm = (insn >>> 16) & 0x1F;
                int option = (insn >>> 13) & 7;
                boolean shift = ((insn >>> 12) & 1) != 0;
                int rn = (insn >>> 5) & 0x1F;
                int rt = insn & 0x1F;
                long idx = getX(rm, false);
                if (option == 2) idx = idx & 0xFFFFFFFFL; // UXTW
                else if (option == 6) idx = (long) (int) idx; // SXTW
                if (shift) idx <<= size;
                long addr = getX(rn, true) + idx;
                execSingleLoadStore(size, opc, rt, addr);
                pc = defaultNextPc;
                return null;
            }

            return "Lệnh ARM64 chưa hỗ trợ: 0x" + Integer.toHexString(insn) + " tại PC=0x" + Long.toHexString(pc);
        }

        void execSingleLoadStore(int size, int opc, int rt, long addr) {
            if (opc == 0) { // STR / STRB / STRH
                long v = getX(rt, false);
                switch (size) {
                    case 0 -> writeByte(addr, (byte) v);
                    case 1 -> writeShort(addr, (short) v);
                    case 2 -> writeInt32(addr, (int) v);
                    case 3 -> writeLong(addr, v);
                }
            } else { // LDR / LDRB / LDRH / LDRSB / LDRSH / LDRSW
                boolean signed = (opc >= 2);
                boolean target64 = (size == 3) || (opc == 2);
                long val = switch (size) {
                    case 0 -> signed ? (long) readByte(addr) : (readByte(addr) & 0xFFL);
                    case 1 -> signed ? (long) readShort(addr) : (readShort(addr) & 0xFFFFL);
                    case 2 -> signed ? (long) readInt32(addr) : (readInt32(addr) & 0xFFFFFFFFL);
                    default -> readLong(addr);
                };
                setX(rt, val, false, target64);
            }
        }

        long execAddSub(long lhs, long rhs, boolean sub, boolean setFlags, boolean sf) {
            if (!sf) {
                int a = (int) lhs;
                int b = (int) rhs;
                int r = sub ? (a - b) : (a + b);
                if (setFlags) {
                    flagN = r < 0;
                    flagZ = r == 0;
                    long ua = a & 0xFFFFFFFFL;
                    long ub = b & 0xFFFFFFFFL;
                    flagC = sub ? (ua >= ub) : ((ua + ub) > 0xFFFFFFFFL);
                    flagV = sub ? (((a ^ b) < 0) && ((a ^ r) < 0)) : (((a ^ ~b) < 0) && ((a ^ r) < 0));
                }
                return r & 0xFFFFFFFFL;
            }
            long r = sub ? (lhs - rhs) : (lhs + rhs);
            if (setFlags) {
                flagN = r < 0;
                flagZ = r == 0;
                flagC = sub ? (Long.compareUnsigned(lhs, rhs) >= 0) : (Long.compareUnsigned(r, lhs) < 0);
                flagV = sub ? (((lhs ^ rhs) < 0) && ((lhs ^ r) < 0)) : (((lhs ^ ~rhs) < 0) && ((lhs ^ r) < 0));
            }
            return r;
        }

        void updateLogicFlags(long res, boolean sf) {
            if (!sf) {
                int r = (int) res;
                flagN = r < 0;
                flagZ = r == 0;
            } else {
                flagN = res < 0;
                flagZ = res == 0;
            }
            flagC = false;
            flagV = false;
        }

        boolean evalCond(int cond) {
            return switch (cond) {
                case 0 -> flagZ;                           // EQ
                case 1 -> !flagZ;                          // NE
                case 2 -> flagC;                           // CS/HS
                case 3 -> !flagC;                          // CC/LO
                case 4 -> flagN;                           // MI
                case 5 -> !flagN;                          // PL
                case 6 -> flagV;                           // VS
                case 7 -> !flagV;                          // VC
                case 8 -> flagC && !flagZ;                 // HI
                case 9 -> !flagC || flagZ;                 // LS
                case 10 -> flagN == flagV;                 // GE
                case 11 -> flagN != flagV;                 // LT
                case 12 -> !flagZ && (flagN == flagV);     // GT
                case 13 -> flagZ || (flagN != flagV);      // LE
                default -> true;                           // AL/NV
            };
        }

        long shiftReg(long v, int shiftType, int imm6, boolean sf) {
            if (!sf) {
                int iv = (int) v;
                int s = imm6 & 31;
                return switch (shiftType) {
                    case 0 -> (iv << s) & 0xFFFFFFFFL;
                    case 1 -> (iv >>> s) & 0xFFFFFFFFL;
                    case 2 -> (iv >> s) & 0xFFFFFFFFL;
                    default -> Integer.rotateRight(iv, s) & 0xFFFFFFFFL;
                };
            }
            int s = imm6 & 63;
            return switch (shiftType) {
                case 0 -> v << s;
                case 1 -> v >>> s;
                case 2 -> v >> s;
                default -> Long.rotateRight(v, s);
            };
        }

        long getX(int reg, boolean allowSp) {
            if (reg == 31) return allowSp ? sp : 0L;
            return regs[reg];
        }

        void setX(int reg, long val, boolean allowSp, boolean sf) {
            long clean = sf ? val : (val & 0xFFFFFFFFL);
            if (reg == 31) {
                if (allowSp) sp = clean;
            } else {
                regs[reg] = clean;
            }
        }

        private byte[] resolveMem(long addr, int[] outOffset) {
            if (addr >= codeStart && addr < codeEnd) {
                outOffset[0] = (int) (addr - codeStart);
                return codeMem;
            }
            if (addr >= STACK_BASE && addr < STACK_BASE + STACK_SIZE) {
                outOffset[0] = (int) (addr - STACK_BASE);
                return stackMem;
            }
            if (addr >= HEAP_BASE && addr < HEAP_BASE + HEAP_SIZE) {
                outOffset[0] = (int) (addr - HEAP_BASE);
                return heapMem;
            }
            if (addr >= JNI_ENV_PTR && addr < JNI_ENV_PTR + jniMem.length) {
                outOffset[0] = (int) (addr - JNI_ENV_PTR);
                return jniMem;
            }
            return null;
        }

        byte readByte(long addr) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            return (m != null && off[0] >= 0 && off[0] < m.length) ? m[off[0]] : 0;
        }

        short readShort(long addr) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m == null || off[0] < 0 || off[0] + 2 > m.length) return 0;
            return ByteBuffer.wrap(m, off[0], 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        int readInt32(long addr) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m == null || off[0] < 0 || off[0] + 4 > m.length) return 0;
            return ByteBuffer.wrap(m, off[0], 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }

        long readLong(long addr) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m == null || off[0] < 0 || off[0] + 8 > m.length) return 0L;
            return ByteBuffer.wrap(m, off[0], 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        }

        void writeByte(long addr, byte val) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m != null && off[0] >= 0 && off[0] < m.length) m[off[0]] = val;
        }

        void writeShort(long addr, short val) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m != null && off[0] >= 0 && off[0] + 2 <= m.length) {
                ByteBuffer.wrap(m, off[0], 2).order(ByteOrder.LITTLE_ENDIAN).putShort(val);
            }
        }

        void writeInt32(long addr, int val) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m != null && off[0] >= 0 && off[0] + 4 <= m.length) {
                ByteBuffer.wrap(m, off[0], 4).order(ByteOrder.LITTLE_ENDIAN).putInt(val);
            }
        }

        void writeLong(long addr, long val) {
            int[] off = new int[1];
            byte[] m = resolveMem(addr, off);
            if (m != null && off[0] >= 0 && off[0] + 8 <= m.length) {
                ByteBuffer.wrap(m, off[0], 8).order(ByteOrder.LITTLE_ENDIAN).putLong(val);
            }
        }

        String readCString(long addr) {
            if (addr == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4096; i++) {
                byte b = readByte(addr + i);
                if (b == 0) break;
                sb.append((char) (b & 0xFF));
            }
            return sb.toString();
        }

        private static long signExtend(long val, int bits) {
            int shift = 64 - bits;
            return (val << shift) >> shift;
        }

        private static String[] parseParamDescriptors(String descriptor) {
            if (descriptor == null) return new String[0];
            int open = descriptor.indexOf('(');
            int close = descriptor.indexOf(')');
            if (open < 0 || close <= open) return new String[0];
            String body = descriptor.substring(open + 1, close);
            java.util.List<String> list = new java.util.ArrayList<>();
            int i = 0;
            while (i < body.length()) {
                int start = i;
                while (i < body.length() && body.charAt(i) == '[') i++;
                if (i < body.length() && body.charAt(i) == 'L') {
                    int semi = body.indexOf(';', i);
                    i = semi >= 0 ? semi + 1 : body.length();
                } else {
                    i++;
                }
                list.add(body.substring(start, i));
            }
            return list.toArray(new String[0]);
        }
    }
}
