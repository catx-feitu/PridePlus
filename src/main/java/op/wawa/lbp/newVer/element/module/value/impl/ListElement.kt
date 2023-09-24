package op.wawa.lbp.newVer.element.module.value.impl

import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import op.wawa.lbp.newVer.ColorManager
import op.wawa.lbp.newVer.MouseUtils
import op.wawa.lbp.newVer.element.module.value.ValueElement
import op.wawa.lbp.newVer.extensions.animSmooth
import org.lwjgl.opengl.GL11.*
import java.awt.Color

class ListElement(val saveValue: ListValue): ValueElement<String>(saveValue) {
    private var expandHeight = 0F
    private var expansion = false

    private val maxSubWidth = -(saveValue.values.map { -Fonts.font40.getStringWidth(it) }.sorted().firstOrNull() ?: 0) + 20

    companion object {
        val expanding = ResourceLocation("wawa/expand.png") }

    override fun drawElement(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float, bgColor: Color, accentColor: Color): Float {
        expandHeight = expandHeight.animSmooth(if (expansion) 16F * (saveValue.values.size - 1F) else 0F, 0.5F) as Float
        val percent = expandHeight / (16F * (saveValue.values.size - 1F))
       Fonts.font40.drawString(value.name, x + 10F, y + 10F - Fonts.font40.fontHeight / 2F + 2F, Color(26, 26, 26).getRGB())
        RenderUtils.drawRoundedRect(x + width - 18F - maxSubWidth, y + 2F, x + width - 10F, y + 18F + expandHeight,
            4, ColorManager.button.rgb)
        GlStateManager.resetColor()
        glPushMatrix()
        glTranslatef(x + width - 20F, y + 10F, 0F)
        glPushMatrix()
        glRotatef(180F * percent, 0F, 0F, 1F)
        glColor4f(1F, 1F, 1F, 1F)
        RenderUtils.drawImage(expanding, -4, -4, 8, 8)
        glPopMatrix()
        glPopMatrix()
       Fonts.font40.drawString(value.get(), x + width - 14F - maxSubWidth, y + 6F, Color(26, 26, 26).getRGB())
        glPushMatrix()
        GlStateManager.translate(x + width - 14F - maxSubWidth, y + 7F, 0F)
        GlStateManager.scale(percent, percent, percent)
        var vertHeight = 0F
        if (percent > 0F) for (subV in unusedValues) {
           Fonts.font40.drawString(subV, 0F, (16F + vertHeight) * percent - 1F, Color(.5F, .5F, .5F, percent.coerceIn(0F, 1F)).rgb)
            vertHeight += 16F
        }
        glPopMatrix()
        valueHeight = 20F + expandHeight
        return valueHeight
    }

    override fun onClick(mouseX: Int, mouseY: Int, x: Float, y: Float, width: Float) {
        if (isDisplayable() && MouseUtils.mouseWithinBounds(mouseX, mouseY, x, y + 2F, x + width, y + 18F))
            expansion = !expansion
        if (expansion) {
            var vertHeight = 0F
            for (subV in unusedValues) {
                if (MouseUtils.mouseWithinBounds(mouseX, mouseY, x + width - 14F - maxSubWidth, y + 18F + vertHeight, x + width - 10F, y + 34F + vertHeight)) {
                    value.set(subV)
                    expansion = false
                    break
                }
                vertHeight += 16F
            }
        }
    }

    val unusedValues: List<String>
        get() = saveValue.values.filter { it != value.get() }
}