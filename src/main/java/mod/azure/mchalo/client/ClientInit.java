package mod.azure.mchalo.client;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import mod.azure.mchalo.MCHaloMod;
import mod.azure.mchalo.client.gui.GunTableScreen;
import mod.azure.mchalo.client.render.BattleRifleRender;
import mod.azure.mchalo.client.render.EnergySwordRender;
import mod.azure.mchalo.client.render.MagnumRender;
import mod.azure.mchalo.client.render.NeedlerRender;
import mod.azure.mchalo.client.render.PropShieldRender;
import mod.azure.mchalo.client.render.RocketLauncherRender;
import mod.azure.mchalo.client.render.ShotgunRender;
import mod.azure.mchalo.client.render.SniperRender;
import mod.azure.mchalo.client.render.projectiles.BulletRender;
import mod.azure.mchalo.client.render.projectiles.NeedleRender;
import mod.azure.mchalo.network.EntityPacket;
import mod.azure.mchalo.util.HaloItems;
import mod.azure.mchalo.util.ProjectilesEntityRegister;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

@SuppressWarnings("deprecation")
public class ClientInit implements ClientModInitializer {

	public static KeyBinding reload = new KeyBinding("key.mchalo.reload", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R,
			"category.mchalo.binds");

	public static KeyBinding scope = new KeyBinding("key.mchalo.scope", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT,
			"category.mchalo.binds");

	@Override
	public void onInitializeClient() {
		ScreenRegistry.register(MCHaloMod.SCREEN_HANDLER_TYPE, GunTableScreen::new);
		KeyBindingHelper.registerKeyBinding(reload);
		KeyBindingHelper.registerKeyBinding(scope);
		GeoItemRenderer.registerItemRenderer(HaloItems.SNIPER, new SniperRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.SHOTGUN, new ShotgunRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.MAGNUM, new MagnumRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.BATTLERIFLE, new BattleRifleRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.ROCKETLAUNCHER, new RocketLauncherRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.PROPSHIELD, new PropShieldRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.ENERGYSWORD, new EnergySwordRender());
		GeoItemRenderer.registerItemRenderer(HaloItems.NEEDLER, new NeedlerRender());
		EntityRendererRegistry.INSTANCE.register(ProjectilesEntityRegister.BULLET, (ctx) -> new BulletRender(ctx));
		EntityRendererRegistry.INSTANCE.register(ProjectilesEntityRegister.NEEDLE, (ctx) -> new NeedleRender(ctx));
		FabricModelPredicateProviderRegistry.register(HaloItems.SNIPER, new Identifier("scoped"),
				(itemStack, clientWorld, livingEntity, seed) -> {
					if (livingEntity != null)
						return isSneaking(livingEntity) ? 1.0F : 0.0F;
					return 0.0F;
				});
		FabricModelPredicateProviderRegistry.register(HaloItems.BATTLERIFLE, new Identifier("scoped"),
				(itemStack, clientWorld, livingEntity, seed) -> {
					if (livingEntity != null)
						return isSneaking(livingEntity) ? 1.0F : 0.0F;
					return 0.0F;
				});
		ClientSidePacketRegistry.INSTANCE.register(EntityPacket.ID, (ctx, buf) -> {
			onPacket(ctx, buf);
		});
	}

	private static boolean isSneaking(LivingEntity livingEntity) {
		return livingEntity.isSneaking();
	}

	public static void requestParticleTexture(Identifier id) {
		ClientSpriteRegistryCallback.event(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
				.register(((texture, registry) -> registry.register(id)));
	}

	@Environment(EnvType.CLIENT)
	public static void onPacket(PacketContext context, PacketByteBuf byteBuf) {
		EntityType<?> type = Registry.ENTITY_TYPE.get(byteBuf.readVarInt());
		UUID entityUUID = byteBuf.readUuid();
		int entityID = byteBuf.readVarInt();
		double x = byteBuf.readDouble();
		double y = byteBuf.readDouble();
		double z = byteBuf.readDouble();
		float pitch = (byteBuf.readByte() * 360) / 256.0F;
		float yaw = (byteBuf.readByte() * 360) / 256.0F;
		context.getTaskQueue().execute(() -> {
			@SuppressWarnings("resource")
			ClientWorld world = MinecraftClient.getInstance().world;
			Entity entity = type.create(world);
			if (entity != null) {
				entity.updatePosition(x, y, z);
				entity.updateTrackedPosition(x, y, z);
				entity.setPitch(pitch);
				entity.setYaw(yaw);
				entity.setId(entityID);
				entity.setUuid(entityUUID);
				world.addEntity(entityID, entity);
			}
		});
	}

}
