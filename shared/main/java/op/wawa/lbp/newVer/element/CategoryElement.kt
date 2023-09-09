package op.wawa.lbp.newVer.element

import net.ccbluex.liquidbounce.LiquidBounce
import op.wawa.lbp.newVer.ColorManager
import op.wawa.lbp.newVer.MouseUtils
import op.wawa.lbp.newVer.element.module.ModuleElement
import op.wawa.lbp.newVer.extensions.animSmooth
import com.mojang.realmsclient.gui.ChatFormatting
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs

class CategoryElement(val category: ModuleCategory): MinecraftInstance() {
    val name = category.displayName
    var focused = false

    private var scrollHeight = 0F
    private var animScrollHeight = 0F
    private var lastHeight = 0F

    val moduleElements = mutableListOf<ModuleElement>()

    init {
        LiquidBounce.moduleManager.modules.filter { it.category == category }.forEach { moduleElements.add(
            ModuleElement(it)
        ) }
    }

    fun drawLabel(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, height: Float) {
        if (focused)
            RenderUtils.drawRoundedRect(x + 3F, y + 3F, x + width - 3F, y + height - 3F, 3, ColorManager.dropDown.rgb)
        else if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y, x + width, y + height))
            RenderUtils.drawRoundedRect(x + 3F, y + 3F, x + width - 3F, y + height - 3F, 3, Color(241, 243, 247).rgb)
        Fonts.font40.drawString(name, x + 10F, y + height / 2F - Fonts.font40.fontHeight / 2F + 2F, Color(26, 26, 26).getRGB())
    }

    fun drawPanel(mX: Int, mY: Int, x: Float, y: Float, width: Float, height: Float, wheel: Int, accentColor: Color) {
        var mouseX = mX
        var mouseY = mY
        lastHeight = 0F
        for (me in moduleElements)
            lastHeight += 40F + me.animHeight
        if (lastHeight >= 10F) lastHeight -= 10F
        handleScrolling(wheel, height)
        drawScroll(x, y + 50F, width, height)
        Fonts.font35.drawString("${ChatFormatting.GRAY}Modules > ${ChatFormatting.RESET}$name", x + 10F, y + 10F, Color(26, 26, 26).getRGB())
        Fonts.font25.drawString("$name", x - 190F, y - 12F, Color(26, 26, 26).getRGB())
        if (mouseY < y + 50F || mouseY >= y + height)
            mouseY = -1
        RenderUtils.makeScissorBox(x, y + 50F, x + width, y + height)
        GL11.glEnable(3089)
        var startY = y + 50F
        for (moduleElement in moduleElements) {
            startY += if (startY + animScrollHeight > y + height || startY + animScrollHeight + 40F + moduleElement.animHeight < y + 50F)
                40F + moduleElement.animHeight
            else
                moduleElement.drawElement(mouseX, mouseY, x, startY + animScrollHeight, width, 40F, accentColor)
        }
        GL11.glDisable(3089)
    }

    private fun handleScrolling(wheel: Int, height: Float) {
        if (wheel != 0) {
            if (wheel > 0)
                scrollHeight += 50F
            else
                scrollHeight -= 50F
        }
        scrollHeight = if (lastHeight > height - 60F)
            scrollHeight.coerceIn(-lastHeight + height - 60F, 0F)
        else
            0F
        animScrollHeight = animScrollHeight.animSmooth(scrollHeight, 0.5F) as Float
    }

    private fun drawScroll(x: Float, y: Float, width: Float, height: Float) {
        if (lastHeight > height - 60F) {
            val last = (height - 60F) - (height - 60F) * ((height - 60F) / lastHeight)
            val multiply = last * abs(animScrollHeight / (-lastHeight + height - 60F)).coerceIn(0F, 1F)
            RenderUtils.drawRoundedRect(x + width - 6F, y + 5F + multiply, x + width - 4F, y + 5F + (height - 60F) * ((height - 60F) / lastHeight) + multiply,
                1, 0x50FFFFFF)
        }
    }

    fun handleMouseClick(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, width: Float, height: Float) {
        var mouseX = mX
        var mouseY = mY
        if (mouseY < y + 50F || mouseY >= y + height)
            mouseY = -1
        var startY = y + 50F
        if (mouseButton == 0)
            for (moduleElement in moduleElements) {
                moduleElement.handleClick(mouseX, mouseY, x, startY + animScrollHeight, width, 40F)
                startY += 40F + moduleElement.animHeight
            }
    }

    fun handleMouseRelease(mX: Int, mY: Int, mouseButton: Int, x: Float, y: Float, width: Float, height: Float) {
        var mouseX = mX
        var mouseY = mY
        if (mouseY < y + 50F || mouseY >= y + height)
            mouseY = -1
        var startY = y + 50F
        if (mouseButton == 0)
            for (moduleElement in moduleElements) {
                moduleElement.handleRelease(mouseX, mouseY, x, startY + animScrollHeight, width, 40F)
                startY += 40F + moduleElement.animHeight
            }
    }

    fun handleKeyTyped(keyTyped: Char, keyCode: Int): Boolean {
        for (moduleElement in moduleElements)
            if (moduleElement.handleKeyTyped(keyTyped, keyCode))
                return true
        return false
    }
}