package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import me.utils.render.BlurBuffer
import me.utils.render.ShadowUtils
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager

import org.lwjgl.opengl.GL11
import java.awt.Color


@ElementInfo(name = "Tomk-KeyBinds")
class TomkKeyBinds : Element() {
    val onlyState = BoolValue("OnlyModuleState", false)
    private val bV = BoolValue("Blur", true)
    private val BlurStrength = FloatValue("BlurStrength", 5f,0f,20f)
    private val bgValue = IntegerValue("Background-Alpha", 120, 0, 255)
    private val radiusValue = FloatValue("Radius", 4.25f, 0f, 10f)
    private val shadowValue = FloatValue("shadow-Value", 10f, 0f, 20f)
    private var anmitY = 0F
    override fun drawElement(): Border? {
        var y2 = 0
        anmitY = RenderUtils.getAnimationState2(anmitY.toDouble(),(17 + getmoduley()).toFloat().toDouble(), 200.0).toFloat()

        //背景
        RenderUtils.drawRoundRect(
                0f,
                0f,
                114f,
                anmitY,
                radiusValue.get(),
                Color(0, 0, 0, bgValue.get()).rgb
        )
        GL11.glTranslated(-renderX, -renderY, 0.0)
        GL11.glScalef( 1F,  1F,  1F)
        GL11.glPushMatrix()
        ShadowUtils.shadow(shadowValue.get(),{
            GL11.glPushMatrix()
            GL11.glTranslated(renderX, renderY, 0.0)
            GL11.glScalef(scale, scale, scale)
            RenderUtils.originalRoundedRect(
                0f,
                0f,
                114f,
                anmitY,
                radiusValue.get(),
                Color(0,0,0).rgb
            )
            GL11.glPopMatrix()

        },{
            GL11.glPushMatrix()
            GL11.glTranslated(renderX, renderY, 0.0)
            GL11.glScalef(scale, scale, scale)
            GlStateManager.enableBlend()
            GlStateManager.disableTexture2D()
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
            RenderUtils.fastRoundedRect(                0f,
                0f,
                114f,
                anmitY,
                radiusValue.get()
            )
            GlStateManager.enableTexture2D()
            GlStateManager.disableBlend()
            GL11.glPopMatrix()
        })
        GL11.glPopMatrix()
        GL11.glScalef(scale, scale, scale)
        GL11.glTranslated(renderX, renderY, 0.0)
        //blur
        if (bV.get()) {
            GL11.glTranslated(-renderX, -renderY, 0.0)
            GL11.glPushMatrix()
            BlurBuffer.CustomBlurRoundArea(
                    renderX.toFloat(),
                    renderY.toFloat(),
                    114F,
                    anmitY,
                    radiusValue.get(), BlurStrength.get()
            )
            GL11.glPopMatrix()
            GL11.glTranslated(renderX, renderY, 0.0)
        }

        //draw Title
        val fwidth = 114f/2F-(Fonts.font35.getStringWidth("KeyBinds")/2F)
        Fonts.posterama40.drawString("KeyBinds", fwidth, 4.5f, -1)

        //draw Module Bind
        for (module in LiquidBounce.moduleManager.modules) {
            if (module.keyBind == 0) continue
            if (onlyState.get()) {
                if (!module.state) continue
            }
            Fonts.posterama30.drawString(module.name, 3f, y2 + 19f, -1)
            Fonts.posterama30.drawString(
                if (module.state) "[True]" else "[False]",
                (108 - Fonts.font35.getStringWidth(  if (module.state)"[True]" else "[False]")).toFloat(),
                y2 + 21f,
                if (module.state) Color(255, 255, 255).rgb else Color(100, 100, 100).rgb
            )
            y2 += 12
        }
        return Border(0f, 0f, 114f, (17 + getmoduley()).toFloat())
    }

    fun getmoduley(): Int {
        var y = 0
        for (module in LiquidBounce.moduleManager.modules) {
            if (module.keyBind == 0) continue
            if (onlyState.get()) {
                if (!module.state) continue
            }
            y += 12
        }
        return y
    }
}