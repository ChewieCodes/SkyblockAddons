package codes.biscuit.skyblockaddons.asm;

import codes.biscuit.skyblockaddons.tweaker.transformer.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

public class EntityPlayerSPTransformer implements Transformer {

    /**
     * {@link net.minecraft.client.entity.EntityPlayerSP}
     */
    @Override
    public String[] getClassName() {
        return new String[]{"net.minecraft.client.entity.EntityPlayerSP"};
    }

    @Override
    public void transform(ClassNode classNode, String name) {
        for (MethodNode methodNode : (List<MethodNode>)classNode.methods) {
            String methodName = mapMethodName(classNode, methodNode);

            // Objective:
            // Find: Method head.
            // Insert:   ReturnValue returnValue = new ReturnValue();
            //           EntityPlayerSPHook.dropOneItemConfirmation(dropAll, returnValue);
            //           if (returnValue.isCancelled()) {
            //               return null;
            //           }

            if (nameMatches(methodName, "dropOneItem","func_71040_bB")) {
                methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), insertConfirmation());
                break;
            }
        }
    }

    private InsnList insertConfirmation() {
        InsnList list = new InsnList();

        list.add(new TypeInsnNode(Opcodes.NEW, "codes/biscuit/skyblockaddons/asm/hooks/ReturnValue"));
        list.add(new InsnNode(Opcodes.DUP)); // ReturnValue returnValue = new ReturnValue();
        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "codes/biscuit/skyblockaddons/asm/hooks/ReturnValue", "<init>", "()V", false));
        list.add(new VarInsnNode(Opcodes.ASTORE, 3));

        list.add(new VarInsnNode(Opcodes.ILOAD, 1));
        list.add(new VarInsnNode(Opcodes.ALOAD, 3)); // EntityPlayerSPHook.dropOneItemConfirmation(dropAll, returnValue);
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "codes/biscuit/skyblockaddons/asm/hooks/EntityPlayerSPHook", "dropOneItemConfirmation",
                "(ZLcodes/biscuit/skyblockaddons/asm/hooks/ReturnValue;)Lnet/minecraft/entity/item/EntityItem;", false));

        list.add(new VarInsnNode(Opcodes.ALOAD, 3));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "codes/biscuit/skyblockaddons/asm/hooks/ReturnValue", "isCancelled",
                "()Z", false));
        LabelNode notCancelled = new LabelNode(); // if (returnValue.isCancelled())
        list.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));

        list.add(new InsnNode(Opcodes.ACONST_NULL)); // return null;
        list.add(new InsnNode(Opcodes.ARETURN));
        list.add(notCancelled);

        return list;
    }
}