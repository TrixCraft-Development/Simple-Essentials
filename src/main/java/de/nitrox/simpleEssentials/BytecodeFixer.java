package de.nitrox.simpleEssentials;

import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;
import java.util.jar.*;

public class BytecodeFixer {

    private static final String TARGET_CLASS = "dev/jorel/commandapi/nms/NMS_1_21_R7.class";

    public static void main(String[] args) throws Exception {
        Path jarPath = Path.of(args[0]);
        Path tmpPath = jarPath.resolveSibling(jarPath.getFileName() + ".tmp");

        try (JarInputStream jin = new JarInputStream(Files.newInputStream(jarPath));
             JarOutputStream jout = new JarOutputStream(Files.newOutputStream(tmpPath))) {

            JarEntry entry;
            while ((entry = jin.getNextJarEntry()) != null) {
                if (entry.getName().equals(TARGET_CLASS)) {
                    byte[] classBytes = jin.readAllBytes();
                    byte[] fixed = fixNMSClass(classBytes);
                    jout.putNextEntry(new JarEntry(entry.getName()));
                    jout.write(fixed);
                } else {
                    jout.putNextEntry(new JarEntry(entry.getName()));
                    jin.transferTo(jout);
                }
                jout.closeEntry();
            }
        }

        Files.delete(jarPath);
        Files.move(tmpPath, jarPath);
    }

    private static byte[] fixNMSClass(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                              String signature, String[] exceptions) {
                if (name.equals("reloadDataPacks") || name.startsWith("lambda$reloadDataPacks$")) {
                    return null;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return cw.toByteArray();
    }
}
