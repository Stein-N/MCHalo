package mod.azure.mchalo.entity.projectiles;

import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager.ControllerRegistrar;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.object.PlayState;
import mod.azure.mchalo.entity.projectiles.helper.CommonHelper;
import mod.azure.mchalo.registry.ModEntities;
import mod.azure.mchalo.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;

public class NeedleEntity extends AbstractArrow implements GeoEntity {

    private static float bulletdamage;
    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);
    public SoundEvent hitSound = this.getDefaultHitGroundSoundEvent();
    private int idleTicks = 0;

    public NeedleEntity(EntityType<? extends NeedleEntity> entityType, Level world) {
        super(entityType, world);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    public NeedleEntity(Level world, Float damage) {
        this(ModEntities.NEEDLE.get(), world);
        bulletdamage = damage;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, event -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("life", (short) this.tickCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getShort("life");
    }

    @Override
    protected void doPostHurtEffects(@NotNull LivingEntity living) {
        super.doPostHurtEffects(living);
        this.remove(Entity.RemovalReason.DISCARDED);
        if (!(living instanceof Player)) {
            living.invulnerableTime = 0;
            living.setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    public void tick() {
        var idleOpt = 100;
        if (getDeltaMovement().lengthSqr() < 0.01) idleTicks++;
        else idleTicks = 0;
        if (idleTicks < idleOpt) super.tick();
        if (this.tickCount >= 40) this.remove(Entity.RemovalReason.DISCARDED);
        CommonHelper.spawnLightSource(this, this.level().isWaterAt(blockPosition()));
        var world = this.level();
        var livingEntities = world.getEntitiesOfClass(Monster.class,
                new AABB(this.getX() - 6.0, this.getY() - 6.0, this.getZ() - 6.0, this.getX() + 6.0, this.getY() + 6.0,
                        this.getZ() + 6.0), entity1 -> entity1 != this.getOwner());
        if (!livingEntities.isEmpty()) {
            var first = livingEntities.getFirst();
            var entityPos = new Vec3(first.getX(), first.getY() + first.getEyeHeight(), first.getZ());
            var distance = entityPos.subtract(this.getX(), this.getY() + this.getEyeHeight(), this.getZ());
            var entityDirect = distance.normalize();
            var arrowDirect = this.getDeltaMovement().normalize();
            var newPath = entityDirect.add(arrowDirect.multiply(4.0, 4.0, 4.0)).normalize();
            var speed = this.getDeltaMovement().length();
            this.setDeltaMovement(newPath.multiply(speed, speed, speed));
        }
    }

    @Override
    protected boolean tryPickup(@NotNull Player player) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return !this.isUnderWater();
    }

    @Override
    public void setSoundEvent(@NotNull SoundEvent soundIn) {
        this.hitSound = soundIn;
    }

    @Override
    protected @NotNull SoundEvent getDefaultHitGroundSoundEvent() {
        return ModSounds.NEEDLER.get();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level().isClientSide) {
            this.remove(Entity.RemovalReason.DISCARDED);
        }
        this.setSoundEvent(ModSounds.NEEDLER.get());
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        var entity = entityHitResult.getEntity();
        if (entityHitResult.getType() != HitResult.Type.ENTITY || !entityHitResult.getEntity().is(
                entity) && !this.level().isClientSide)
            this.remove(Entity.RemovalReason.DISCARDED);
        var entity2 = this.getOwner();
        DamageSource damageSource2;
        if (entity2 == null) damageSource2 = damageSources().arrow(this, this);
        else {
            damageSource2 = damageSources().arrow(this, entity2);
            if (entity2 instanceof LivingEntity livingEntity) livingEntity.setLastHurtMob(entity);
        }
        if (entity.hurt(damageSource2, bulletdamage)) {
            if (entity instanceof LivingEntity livingEntity) {
                if (!this.level().isClientSide) {
                    livingEntity.setArrowCount(livingEntity.getArrowCount() + 1);
                }
                this.doPostHurtEffects(livingEntity);
                if (livingEntity != entity2 && livingEntity instanceof Player && entity2 instanceof ServerPlayer && !this.isSilent())
                    ((ServerPlayer) entity2).connection.send(
                            new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER,
                                    ClientboundGameEventPacket.DEMO_PARAM_INTRO));
                this.remove(RemovalReason.KILLED);
            }
        } else {
            if (!this.level().isClientSide) this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public @NotNull ItemStack getPickupItem() {
        return new ItemStack(Items.AIR);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return Items.AIR.getDefaultInstance();
    }

}