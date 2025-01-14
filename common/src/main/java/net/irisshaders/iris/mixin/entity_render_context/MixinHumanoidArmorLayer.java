package net.irisshaders.iris.mixin.entity_render_context;

import com.mojang.blaze3d.vertex.PoseStack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>>
	extends RenderLayer<T, M> {
	private int backupValue = 0;

	public MixinHumanoidArmorLayer(RenderLayerParent<T, M> pRenderLayer0) {
		super(pRenderLayer0);
	}

	@Inject(method = "renderArmorPiece", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;copyPropertiesTo(Lnet/minecraft/client/model/HumanoidModel;)V"))
	private void changeId(PoseStack poseStack, MultiBufferSource multiBufferSource, T livingEntity, EquipmentSlot equipmentSlot, int i, A humanoidModel, CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;

		ResourceLocation location = BuiltInRegistries.ITEM.getKey(livingEntity.getItemBySlot(equipmentSlot).getItem());

		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId(location.getNamespace(), location.getPath())));
	}

	@Inject(method = "renderTrim", at = @At(value = "HEAD"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void changeTrimTemp(ArmorMaterial armorMaterial, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ArmorTrim armorTrim, A humanoidModel, boolean bl, CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;

		backupValue = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt(new NamespacedId("minecraft", "trim_" + armorTrim.material().value().assetName())));
	}

	@Inject(method = "renderTrim", at = @At(value = "TAIL"))
	private void changeTrimTemp2(ArmorMaterial armorMaterial, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, ArmorTrim armorTrim, A humanoidModel, boolean bl, CallbackInfo ci) {
		if (WorldRenderingSettings.INSTANCE.getItemIds() == null) return;
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(backupValue);
		backupValue = 0;
	}

	@Inject(method = "renderArmorPiece", at = @At(value = "TAIL"))
	private void changeId2(CallbackInfo ci) {
		CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
	}
}
