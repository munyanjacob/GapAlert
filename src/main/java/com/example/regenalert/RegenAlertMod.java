package com.example.regenalert;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;

@Mod(modid = RegenAlertMod.MODID, version = RegenAlertMod.VERSION)
public class RegenAlertMod {
    public static final String MODID = "regenalert";
    public static final String VERSION = "1.0";

    private boolean hadRegen2 = false;
    private int alertTimer = 0;
    private final int ALERT_DURATION = 60; // Alert display time in ticks (3 seconds)

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player != Minecraft.getMinecraft().thePlayer) {
            return;
        }

        boolean hasRegen2Now = false;

        // Check for Regeneration V effect
        PotionEffect regenEffect = event.player.getActivePotionEffect(Potion.regeneration);
        if (regenEffect != null && regenEffect.getAmplifier() >= 4) { // Amplifier 4 = Regen V (0-based index)
            hasRegen2Now = true;
        }

        // If player had Regen V before but not anymore, trigger alert
        if (hadRegen2 && !hasRegen2Now) {
            alertTimer = ALERT_DURATION;
            // Send chat message
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "❗ " +
                            EnumChatFormatting.GOLD + "GAP" +
                            EnumChatFormatting.RED + " GONE!"));
        }

        hadRegen2 = hasRegen2Now;

        // Decrease alert timer
        if (alertTimer > 0) {
            alertTimer--;
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // Only render our alert if the timer is active and this is the TEXT layer
        if (alertTimer > 0 && event.type == RenderGameOverlayEvent.ElementType.TEXT) {
            Minecraft mc = Minecraft.getMinecraft();
            FontRenderer fontRenderer = mc.fontRendererObj;
            ScaledResolution scaled = new ScaledResolution(mc);

            // Calculate centered position
            int screenWidth = scaled.getScaledWidth();
            int screenHeight = scaled.getScaledHeight();

            // Define alert text
            String alertText = "§c❗ §6REGEN V EXPIRED §c❗";

            // Calculate width to center the text
            int textWidth = fontRenderer.getStringWidth(alertText);
            int x = (screenWidth - textWidth) / 2;
            int y = screenHeight / 2 - 40; // Position closer to the middle of the screen

            // Draw a semi-transparent background with extra padding for the larger text
            int bgColor = new Color(0, 0, 0, 128).getRGB();  // Semi-transparent black
            int textPadding = 8; // Increased padding for larger text
            drawRect(x - textPadding, y - textPadding,
                    x + textWidth + textPadding, y + fontRenderer.FONT_HEIGHT + textPadding,
                    bgColor);

            // Draw the alert text - larger and more prominent
            org.lwjgl.opengl.GL11.glPushMatrix();
            org.lwjgl.opengl.GL11.glScalef(2.0f, 2.0f, 2.0f);
            fontRenderer.drawStringWithShadow(alertText, x / 2.0f, y / 2.0f, 0xFFFFFF);
            org.lwjgl.opengl.GL11.glPopMatrix();

            // Add pulsing effect based on timer
            if (alertTimer > ALERT_DURATION - 15) {
                int pulseSize = 16;
                String pulseText = "§c❗ §6GAP GONE §c❗";
                float scaleFactor = 2.0f + (float)(ALERT_DURATION - alertTimer) / 30.0f; // Increased base scale for bigger alert

                // Draw pulsing text (larger version that fades quickly)
                int pulseWidth = (int)(fontRenderer.getStringWidth(pulseText) * scaleFactor);
                int pulseX = (screenWidth - pulseWidth) / 2;
                int pulseY = y - (int)((fontRenderer.FONT_HEIGHT * scaleFactor - fontRenderer.FONT_HEIGHT) / 2);

                // Calculate alpha based on remaining time of the pulse
                int alpha = 255 - (int)(255 * (float)(ALERT_DURATION - alertTimer) / 15.0f);
                alpha = Math.max(0, Math.min(255, alpha));

                // Draw with scaling and fading
                drawScaledText(fontRenderer, pulseText, pulseX, pulseY, 0xFFFFFF | (alpha << 24), scaleFactor);
            }
        }
    }

    private void drawScaledText(FontRenderer fr, String text, int x, int y, int color, float scale) {
        Minecraft mc = Minecraft.getMinecraft();
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glScalef(scale, scale, scale);
        float scaleReciprocal = 1.0f / scale;
        fr.drawStringWithShadow(text, x * scaleReciprocal, y * scaleReciprocal, color);
        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        org.lwjgl.opengl.GL11.glColor4f(red, green, blue, alpha);

        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
        org.lwjgl.opengl.GL11.glVertex2f(left, bottom);
        org.lwjgl.opengl.GL11.glVertex2f(right, bottom);
        org.lwjgl.opengl.GL11.glVertex2f(right, top);
        org.lwjgl.opengl.GL11.glVertex2f(left, top);
        org.lwjgl.opengl.GL11.glEnd();

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
    }
}