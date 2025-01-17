package thut.bling.client.render;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelCustom;

public class Neck
{

    public static void renderNeck(final PoseStack mat, final MultiBufferSource buff, final LivingEntity wearer,
            final ItemStack stack, final IModel model, final ResourceLocation[] textures, final int brightness,
            final int overlay)
    {
        if (!(model instanceof IModelCustom)) return;
        if (!model.isLoaded() || !model.isValid()) return;
        final ResourceLocation[] tex = textures.clone();
        final IModelCustom renderable = (IModelCustom) model;
        DyeColor ret;
        Color colour;
        float s, dx, dy, dz;
        dx = 0;
        dy = -.0f;
        dz = -0.03f;
        s = 0.5f;
        mat.mulPose(Vector3f.XP.rotationDegrees(90));
        mat.mulPose(Vector3f.ZP.rotationDegrees(180));
        mat.translate(dx, dy, dz);
        mat.scale(s, s, s);
        final String colorpart = "main";
        final String itempart = "gem";
        ret = DyeColor.YELLOW;
        if (stack.hasTag() && stack.getTag().contains("dyeColour"))
        {
            final int damage = stack.getTag().getInt("dyeColour");
            ret = DyeColor.byId(damage);
        }
        colour = new Color(ret.getTextColor() + 0xFF000000);
        IExtendedModelPart part = model.getParts().get(colorpart);
        if (part != null)
        {
            part.setRGBABrO(colour.getRed(), colour.getGreen(), colour.getBlue(), 255, brightness, overlay);
            mat.scale(1, 1, .1f);
            final VertexConsumer buf1 = Util.makeBuilder(buff, tex[1]);
            renderable.renderPart(mat, buf1, colorpart);
        }
        part = model.getParts().get(itempart);
        if (stack.hasTag() && stack.getTag().contains("gem"))
            tex[0] = new ResourceLocation(stack.getTag().getString("gem"));
        else tex[0] = null;
        if (part != null && tex[0] != null)
        {
            final VertexConsumer buf0 = Util.makeBuilder(buff, tex[0]);
            mat.scale(1, 1, 10);
            mat.translate(0, 0.01, -0.075);
            renderable.renderPart(mat, buf0, itempart);
        }
    }

}
