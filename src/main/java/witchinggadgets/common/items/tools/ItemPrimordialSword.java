package witchinggadgets.common.items.tools;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.IRepairable;
import thaumcraft.api.aspects.Aspect;
import witchinggadgets.api.IPrimordialCrafting;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.items.interfaces.IItemEvent;
import witchinggadgets.common.util.Lib;
import witchinggadgets.common.util.Utilities;

public class ItemPrimordialSword extends ItemSword
        implements IPrimordialCrafting, IRepairable, IItemEvent, IPrimordialGear {

    IIcon overlay;

    public ItemPrimordialSword(ToolMaterial mat) {
        super(mat);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean equipped) {
        super.onUpdate(stack, world, entity, slot, equipped);
        if ((stack.isItemDamaged()) && (entity != null)
                && (entity.ticksExisted % 40 == 0)
                && ((entity instanceof EntityLivingBase)))
            stack.damageItem(-1, (EntityLivingBase) entity);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity target) {
        if (target instanceof EntityLivingBase targetEntity) {
            if (getAbility(stack) == 0) {
                for (EntityLivingBase e : player.worldObj.getEntitiesWithinAABB(
                        EntityLivingBase.class,
                        AxisAlignedBB.getBoundingBox(
                                target.posX - 2,
                                target.posY - 2,
                                target.posZ - 2,
                                target.posX + 2,
                                target.posY + 2,
                                target.posZ + 2)))
                    if (e.canAttackWithItem() && !e.hitByEntity(player) && !e.equals(player)) {
                        float f = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
                                .getAttributeValue();
                        int i = EnchantmentHelper.getKnockbackModifier(player, e);
                        float f1 = EnchantmentHelper.getEnchantmentModifierLiving(player, e);
                        if (player.isSprinting()) ++i;
                        if (f > 0 || f1 > 0) {
                            boolean flag = player.fallDistance > 0.0F && !player.onGround
                                    && !player.isOnLadder()
                                    && !player.isInWater()
                                    && !player.isPotionActive(Potion.blindness)
                                    && player.ridingEntity == null
                                    && e instanceof EntityLivingBase;
                            if (flag && f > 0) f *= 1.5F;

                            f += f1;
                            boolean flag1 = false;
                            int j = EnchantmentHelper.getFireAspectModifier(player);

                            if (j > 0 && !e.isBurning()) {
                                flag1 = true;
                                e.setFire(1);
                            }

                            boolean flag2 = e.attackEntityFrom(DamageSource.causePlayerDamage(player), f);

                            if (flag2) {
                                if (i > 0) {
                                    e.addVelocity(
                                            -MathHelper.sin(player.rotationYaw * (float) Math.PI / 180.0F) * (float) i
                                                    * 0.5F,
                                            0.1D,
                                            MathHelper.cos(player.rotationYaw * (float) Math.PI / 180.0F) * (float) i
                                                    * 0.5F);
                                    player.motionX *= 0.6D;
                                    player.motionZ *= 0.6D;
                                    player.setSprinting(false);
                                }

                                if (flag) player.onCriticalHit(e);
                                if (f1 > 0) player.onEnchantmentCritical(e);
                                if (f >= 18) player.triggerAchievement(AchievementList.overkill);

                                player.setLastAttacker(e);
                                EnchantmentHelper.func_151384_a(e, player);
                                EnchantmentHelper.func_151385_b(player, e);
                                player.addStat(StatList.damageDealtStat, Math.round(f * 10.0F));
                                if (j > 0) e.setFire(j * 4);
                                player.addExhaustion(0.3F);
                            } else if (flag1) e.extinguish();
                        }
                    }
            }
            if (getAbility(stack) == 2) {
                targetEntity.addPotionEffect(new PotionEffect(WGContent.pot_cinderCoat.id, 80, 1));
                target.setFire(4);
            }
            if (getAbility(stack) == 3)
                targetEntity.addPotionEffect(new PotionEffect(WGContent.pot_dissolve.id, 80, 2));
            if (getAbility(stack) == 5) {
                targetEntity.addPotionEffect(new PotionEffect(Potion.weakness.getId(), 60));
                targetEntity.addPotionEffect(new PotionEffect(Potion.hunger.getId(), 120));
            }
        }
        return false;
    }

    @Override
    public void onUserDamaged(LivingHurtEvent event, ItemStack stack) {
        if (getAbility(stack) == 1 && ((EntityPlayer) event.entityLiving).isBlocking()) {
            int time = event.entityLiving.getActivePotionEffect(Potion.resistance) != null
                    ? event.entityLiving.getActivePotionEffect(Potion.resistance).getDuration()
                    : 0;
            time = Math.min(time + 30, 80);
            int amp = event.entityLiving.getActivePotionEffect(Potion.resistance) != null
                    ? event.entityLiving.getActivePotionEffect(Potion.resistance).getAmplifier()
                    : -1;
            amp = Math.min(amp + 1, 2);
            event.entityLiving.addPotionEffect(new PotionEffect(Potion.resistance.id, time, amp));
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack aStack, World aWorld, EntityPlayer aPlayer) {
        if (aPlayer.isSneaking() && !aPlayer.worldObj.isRemote) {
            cycleAbilities(aStack);
            return aStack;
        } else return super.onItemRightClick(aStack, aWorld, aPlayer);
    }

    @Override
    public boolean onItemUse(ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY, int aZ, int aSide,
            float a, float b, float c) {

        return super.onItemUse(aStack, aPlayer, aWorld, aX, aY, aZ, aSide, a, b, c);
    }

    @Override
    public int getReturnedPearls(ItemStack stack) {
        return 2;
    }

    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean par4) {
        int ab = getAbility(stack);
        String add = ab >= 0 && ab < 6 ? " " + EnumChatFormatting.DARK_GRAY
                + "- \u00a7"
                + Aspect.getPrimalAspects().get(ab).getChatcolor()
                + Aspect.getPrimalAspects().get(ab).getName()
                + EnumChatFormatting.RESET : "";

        list.add(EnumChatFormatting.DARK_GRAY + StatCollector.translateToLocal("wg.desc.primal") + add);
        GameSettings keybind = Minecraft.getMinecraft().gameSettings;
        list.add(
                StatCollector.translateToLocal(Lib.DESCRIPTION + "cycleArmor")
                        .replaceAll(
                                "%s1",
                                StatCollector.translateToLocalFormatted(
                                        GameSettings.getKeyDisplayString(keybind.keyBindSneak.getKeyCode())))
                        .replaceAll(
                                "%s2",
                                StatCollector.translateToLocalFormatted(
                                        GameSettings.getKeyDisplayString(keybind.keyBindUseItem.getKeyCode()))));
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.epic;
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        this.itemIcon = iconRegister.registerIcon("witchinggadgets:primordialSword");
        this.overlay = iconRegister.registerIcon("witchinggadgets:primordialSword_overlay");
    }

    @Override
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    public int getRenderPasses(int meta) {
        return 2;
    }

    @Override
    public IIcon getIconFromDamageForRenderPass(int par1, int pass) {
        if (pass == 0) return itemIcon;
        return overlay;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        if (pass == 1) {
            int ab = getAbility(stack);
            if (ab >= 0 && ab < 6) return Aspect.getPrimalAspects().get(getAbility(stack)).getColor();
        }
        return 0xffffff;
    }

    @Override
    public void cycleAbilities(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        int cur = stack.getTagCompound().getInteger("currentMode");
        cur++;
        if (cur >= 6) cur = 0;
        stack.getTagCompound().setInteger("currentMode", cur);
    }

    @Override
    public int getAbility(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound().getInteger("currentMode");
    }

    @Override
    public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
        return Utilities.compareToOreName(stack2, "ingotVoid");
    }

    @Override
    public void onUserAttacking(AttackEntityEvent event, ItemStack stack) {}

    @Override
    public void onUserJump(LivingJumpEvent event, ItemStack stack) {}

    @Override
    public void onUserFall(LivingFallEvent event, ItemStack stack) {}

    @Override
    public void onUserTargeted(LivingSetAttackTargetEvent event, ItemStack stack) {}
}
