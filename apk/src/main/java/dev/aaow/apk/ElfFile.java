package dev.aaow.apk;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bộ phân tích định dạng nhị phân ELF (ELF32 & ELF64) cho các thư viện native (.so) bên trong APK.
 * Trích xuất kiến trúc CPU (ARM64/ARMv7/x86_64/x86), phụ thuộc DT_NEEDED,
 * danh sách ký hiệu hàm JNI (JNI_OnLoad, Java_*) và ánh xạ phân đoạn PT_LOAD.
 */
public final class ElfFile {
    public static final int ELFCLASS32 = 1;
    public static final int ELFCLASS64 = 2;
    public static final int ELFDATA2LSB = 1;

    public static final int EM_386 = 3;
    public static final int EM_ARM = 40;
    public static final int EM_X86_64 = 62;
    public static final int EM_AARCH64 = 183;

    private static final int PT_LOAD = 1;
    private static final int SHT_SYMTAB = 2;
    private static final int SHT_STRTAB = 3;
    private static final int SHT_DYNAMIC = 6;
    private static final int SHT_DYNSYM = 11;
    private static final int DT_NULL = 0;
    private static final int DT_NEEDED = 1;
    private static final int STT_FUNC = 2;

    public record ProgramSegment(long vaddr, long offset, long filesz, long memsz, int flags) {
    }

    public record ElfSymbol(String name, long address, long size, int type, int sectionIndex) {
        public boolean isFunction() {
            return type == STT_FUNC;
        }

        public boolean isDefined() {
            return sectionIndex != 0;
        }

        public boolean isJniSymbol() {
            return isDefined() && (name.equals("JNI_OnLoad") || name.equals("JNI_OnUnload") || name.startsWith("Java_"));
        }
    }

    /**
     * Ảnh bộ nhớ ảo hợp nhất từ các phân đoạn PT_LOAD của file ELF, phục vụ trình thông dịch ARM64/kiểm tra dữ liệu.
     */
    public static final class VirtualImage {
        private final long baseAddress;
        private final long minVaddr;
        private final long maxVaddr;
        private final byte[] memory;

        VirtualImage(long baseAddress, long minVaddr, long maxVaddr, byte[] memory) {
            this.baseAddress = baseAddress;
            this.minVaddr = minVaddr;
            this.maxVaddr = maxVaddr;
            this.memory = memory;
        }

        public long getBaseAddress() {
            return baseAddress;
        }

        public long getMinVaddr() {
            return minVaddr;
        }

        public long getMaxVaddr() {
            return maxVaddr;
        }

        public long mapAddress(long elfVaddr) {
            return baseAddress + (elfVaddr - minVaddr);
        }

        public byte[] getMemory() {
            return memory;
        }

        public int size() {
            return memory.length;
        }
    }

    private final byte[] rawBytes;
    private final int elfClass;
    private final int endianness;
    private final int machine;
    private final int elfType;
    private final long entryPoint;
    private final List<ProgramSegment> loadSegments;
    private final List<String> neededLibraries;
    private final List<ElfSymbol> exportedSymbols;
    private final Map<String, ElfSymbol> symbolByName;

    private ElfFile(byte[] rawBytes, int elfClass, int endianness, int machine, int elfType, long entryPoint,
                    List<ProgramSegment> loadSegments, List<String> neededLibraries, List<ElfSymbol> exportedSymbols) {
        this.rawBytes = rawBytes;
        this.elfClass = elfClass;
        this.endianness = endianness;
        this.machine = machine;
        this.elfType = elfType;
        this.entryPoint = entryPoint;
        this.loadSegments = Collections.unmodifiableList(loadSegments);
        this.neededLibraries = Collections.unmodifiableList(neededLibraries);
        this.exportedSymbols = Collections.unmodifiableList(exportedSymbols);
        Map<String, ElfSymbol> map = new LinkedHashMap<>();
        for (ElfSymbol s : exportedSymbols) {
            map.putIfAbsent(s.name(), s);
        }
        this.symbolByName = Collections.unmodifiableMap(map);
    }

    public static boolean isElf(byte[] data) {
        return data != null && data.length >= 52
                && data[0] == 0x7F && data[1] == 'E' && data[2] == 'L' && data[3] == 'F';
    }

    public static ElfFile parse(byte[] data) {
        if (!isElf(data)) {
            throw new IllegalArgumentException("Không phải file nhị phân ELF hợp lệ");
        }
        int elfClass = data[4] & 0xFF;
        int endian = data[5] & 0xFF;
        ByteOrder order = endian == ELFDATA2LSB ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer buf = ByteBuffer.wrap(data).order(order);

        int elfType = Short.toUnsignedInt(buf.getShort(16));
        int machine = Short.toUnsignedInt(buf.getShort(18));

        long entry;
        long phoff;
        long shoff;
        int phentsize;
        int phnum;
        int shentsize;
        int shnum;
        if (elfClass == ELFCLASS64) {
            if (data.length < 64) throw new IllegalArgumentException("ELF64 header bị cắt cụt");
            entry = buf.getLong(24);
            phoff = buf.getLong(32);
            shoff = buf.getLong(40);
            phentsize = Short.toUnsignedInt(buf.getShort(54));
            phnum = Short.toUnsignedInt(buf.getShort(56));
            shentsize = Short.toUnsignedInt(buf.getShort(58));
            shnum = Short.toUnsignedInt(buf.getShort(60));
        } else if (elfClass == ELFCLASS32) {
            entry = Integer.toUnsignedLong(buf.getInt(24));
            phoff = Integer.toUnsignedLong(buf.getInt(28));
            shoff = Integer.toUnsignedLong(buf.getInt(32));
            phentsize = Short.toUnsignedInt(buf.getShort(42));
            phnum = Short.toUnsignedInt(buf.getShort(44));
            shentsize = Short.toUnsignedInt(buf.getShort(46));
            shnum = Short.toUnsignedInt(buf.getShort(48));
        } else {
            throw new IllegalArgumentException("Không hỗ trợ ELF class: " + elfClass);
        }

        List<ProgramSegment> segments = parseProgramSegments(buf, data.length, elfClass, phoff, phentsize, phnum);
        List<ElfSymbol> symbols = new ArrayList<>();
        List<String> needed = new ArrayList<>();
        parseSections(buf, data, elfClass, shoff, shentsize, shnum, symbols, needed);

        return new ElfFile(data, elfClass, endian, machine, elfType, entry, segments, needed, symbols);
    }

    private static List<ProgramSegment> parseProgramSegments(ByteBuffer buf, int len, int elfClass,
                                                             long phoff, int phentsize, int phnum) {
        List<ProgramSegment> list = new ArrayList<>();
        if (phoff <= 0 || phnum <= 0 || phentsize <= 0) return list;
        for (int i = 0; i < phnum; i++) {
            int off = (int) (phoff + (long) i * phentsize);
            if (off < 0 || off + phentsize > len) break;
            int type = buf.getInt(off);
            if (type != PT_LOAD) continue;
            if (elfClass == ELFCLASS64 && phentsize >= 56) {
                int flags = buf.getInt(off + 4);
                long pOffset = buf.getLong(off + 8);
                long vaddr = buf.getLong(off + 16);
                long filesz = buf.getLong(off + 32);
                long memsz = buf.getLong(off + 40);
                list.add(new ProgramSegment(vaddr, pOffset, filesz, memsz, flags));
            } else if (elfClass == ELFCLASS32 && phentsize >= 32) {
                long pOffset = Integer.toUnsignedLong(buf.getInt(off + 4));
                long vaddr = Integer.toUnsignedLong(buf.getInt(off + 8));
                long filesz = Integer.toUnsignedLong(buf.getInt(off + 16));
                long memsz = Integer.toUnsignedLong(buf.getInt(off + 20));
                int flags = buf.getInt(off + 24);
                list.add(new ProgramSegment(vaddr, pOffset, filesz, memsz, flags));
            }
        }
        return list;
    }

    private static void parseSections(ByteBuffer buf, byte[] data, int elfClass,
                                      long shoff, int shentsize, int shnum,
                                      List<ElfSymbol> symbolsOut, List<String> neededOut) {
        if (shoff <= 0 || shnum <= 0 || shentsize <= 0) return;
        int len = data.length;
        int[] shTypes = new int[shnum];
        long[] shOffsets = new long[shnum];
        long[] shSizes = new long[shnum];
        int[] shLinks = new int[shnum];
        long[] shEntSizes = new long[shnum];

        for (int i = 0; i < shnum; i++) {
            int off = (int) (shoff + (long) i * shentsize);
            if (off < 0 || off + shentsize > len) break;
            shTypes[i] = buf.getInt(off + 4);
            if (elfClass == ELFCLASS64 && shentsize >= 64) {
                shOffsets[i] = buf.getLong(off + 24);
                shSizes[i] = buf.getLong(off + 32);
                shLinks[i] = buf.getInt(off + 40);
                shEntSizes[i] = buf.getLong(off + 56);
            } else if (elfClass == ELFCLASS32 && shentsize >= 40) {
                shOffsets[i] = Integer.toUnsignedLong(buf.getInt(off + 16));
                shSizes[i] = Integer.toUnsignedLong(buf.getInt(off + 20));
                shLinks[i] = buf.getInt(off + 24);
                shEntSizes[i] = Integer.toUnsignedLong(buf.getInt(off + 36));
            }
        }

        for (int i = 0; i < shnum; i++) {
            int type = shTypes[i];
            if ((type == SHT_DYNSYM || type == SHT_SYMTAB) && shEntSizes[i] > 0) {
                int link = shLinks[i];
                if (link < 0 || link >= shnum || shTypes[link] != SHT_STRTAB) continue;
                int strOff = (int) shOffsets[link];
                int strSize = (int) shSizes[link];
                int count = (int) (shSizes[i] / shEntSizes[i]);
                for (int j = 0; j < count; j++) {
                    int symOff = (int) (shOffsets[i] + (long) j * shEntSizes[i]);
                    if (symOff < 0 || symOff + shEntSizes[i] > len) break;
                    int stName = buf.getInt(symOff);
                    int stType;
                    int stShndx;
                    long stValue;
                    long stSize;
                    if (elfClass == ELFCLASS64) {
                        stType = data[symOff + 4] & 0x0F;
                        stShndx = Short.toUnsignedInt(buf.getShort(symOff + 6));
                        stValue = buf.getLong(symOff + 8);
                        stSize = buf.getLong(symOff + 16);
                    } else {
                        stValue = Integer.toUnsignedLong(buf.getInt(symOff + 4));
                        stSize = Integer.toUnsignedLong(buf.getInt(symOff + 8));
                        stType = data[symOff + 12] & 0x0F;
                        stShndx = Short.toUnsignedInt(buf.getShort(symOff + 14));
                    }
                    if (stShndx == 0 || stName <= 0 || stName >= strSize) continue;
                    String symName = readCString(data, strOff + stName, strOff + strSize);
                    if (!symName.isEmpty()) {
                        symbolsOut.add(new ElfSymbol(symName, stValue, stSize, stType, stShndx));
                    }
                }
            } else if (type == SHT_DYNAMIC) {
                int link = shLinks[i];
                if (link < 0 || link >= shnum || shTypes[link] != SHT_STRTAB) continue;
                int strOff = (int) shOffsets[link];
                int strSize = (int) shSizes[link];
                int entSz = elfClass == ELFCLASS64 ? 16 : 8;
                int count = (int) (shSizes[i] / entSz);
                for (int j = 0; j < count; j++) {
                    int dynOff = (int) (shOffsets[i] + (long) j * entSz);
                    if (dynOff < 0 || dynOff + entSz > len) break;
                    long tag = elfClass == ELFCLASS64 ? buf.getLong(dynOff) : buf.getInt(dynOff);
                    long val = elfClass == ELFCLASS64 ? buf.getLong(dynOff + 8) : Integer.toUnsignedLong(buf.getInt(dynOff + 4));
                    if (tag == DT_NULL) break;
                    if (tag == DT_NEEDED && val >= 0 && val < strSize) {
                        String lib = readCString(data, strOff + (int) val, strOff + strSize);
                        if (!lib.isEmpty() && !neededOut.contains(lib)) {
                            neededOut.add(lib);
                        }
                    }
                }
            }
        }
    }

    private static String readCString(byte[] data, int start, int limit) {
        if (start < 0 || start >= data.length) return "";
        int max = Math.min(limit, data.length);
        int end = start;
        while (end < max && data[end] != 0) end++;
        return new String(data, start, end - start, StandardCharsets.UTF_8);
    }

    public VirtualImage buildVirtualMemoryImage(long baseAddress) {
        if (loadSegments.isEmpty()) {
            return new VirtualImage(baseAddress, 0, 0, new byte[0]);
        }
        long minV = Long.MAX_VALUE;
        long maxV = 0;
        for (ProgramSegment seg : loadSegments) {
            minV = Math.min(minV, seg.vaddr());
            maxV = Math.max(maxV, seg.vaddr() + Math.max(seg.filesz(), seg.memsz()));
        }
        long span = maxV - minV;
        if (span <= 0 || span > 64L * 1024 * 1024) {
            return new VirtualImage(baseAddress, minV, minV, new byte[0]);
        }
        byte[] mem = new byte[(int) span];
        for (ProgramSegment seg : loadSegments) {
            int dstOff = (int) (seg.vaddr() - minV);
            int srcOff = (int) seg.offset();
            int copyLen = (int) Math.min(seg.filesz(), Math.max(0, rawBytes.length - srcOff));
            if (srcOff >= 0 && copyLen > 0 && dstOff + copyLen <= mem.length) {
                System.arraycopy(rawBytes, srcOff, mem, dstOff, copyLen);
            }
        }
        return new VirtualImage(baseAddress, minV, maxV, mem);
    }

    public int getElfClass() {
        return elfClass;
    }

    public int getMachine() {
        return machine;
    }

    public String getMachineName() {
        return switch (machine) {
            case EM_AARCH64 -> "arm64-v8a";
            case EM_ARM -> "armeabi-v7a";
            case EM_X86_64 -> "x86_64";
            case EM_386 -> "x86";
            default -> "unknown(" + machine + ")";
        };
    }

    public boolean is64Bit() {
        return elfClass == ELFCLASS64;
    }

    public long getEntryPoint() {
        return entryPoint;
    }

    public List<ProgramSegment> getLoadSegments() {
        return loadSegments;
    }

    public List<String> getNeededLibraries() {
        return neededLibraries;
    }

    public List<ElfSymbol> getExportedSymbols() {
        return exportedSymbols;
    }

    public List<ElfSymbol> getJniSymbols() {
        List<ElfSymbol> out = new ArrayList<>();
        for (ElfSymbol s : exportedSymbols) {
            if (s.isJniSymbol()) out.add(s);
        }
        return out;
    }

    public ElfSymbol findSymbol(String name) {
        return symbolByName.get(name);
    }

    /** Chuyển tên phương thức Java thành tên hàm C JNI chuẩn (không overload). */
    public static String toJniSymbolName(String fqcn, String methodName) {
        return "Java_" + mangleJniPart(fqcn.replace('/', '.')) + "_" + mangleJniPart(methodName);
    }

    private static String mangleJniPart(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                sb.append('_');
            } else if (c == '_') {
                sb.append("_1");
            } else if (c == ';') {
                sb.append("_2");
            } else if (c == '[') {
                sb.append("_3");
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append(String.format("_0%04x", (int) c));
            }
        }
        return sb.toString();
    }
}
