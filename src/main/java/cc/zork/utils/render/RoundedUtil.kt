package cc.zork.utils.render

import cc.zork.features.ui.HUD
import cc.zork.utils.mc
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import cc.zork.utils.render.shader.BloomUtil
import cc.zork.utils.render.shader.GaussianBlur
import cc.zork.utils.render.shader.KawaseBlur
import cc.zork.utils.render.shader.ShaderUtil
import org.lwjgl.opengl.GL11
import java.awt.Color

object RoundedUtil {
    var roundedShader = ShaderUtil("roundedRect")
    var roundedOutlineShader = ShaderUtil("client/shaders/roundRectOutline.frag")
    private val roundedTexturedShader = ShaderUtil("client/shaders/roundRectTextured.frag")
    private val roundedGradientShader = ShaderUtil("roundedRectGradient")
    var bloomFramebuffer: Framebuffer? = Framebuffer(1, 1, false);
    @JvmStatic
    fun drawRound(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color) {
        drawRound(x, y, width, height, radius, false, color)
    }

    @JvmStatic
    fun createFrameBuffer(framebuffer: Framebuffer?): Framebuffer {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            framebuffer?.deleteFramebuffer()
            return Framebuffer(mc.displayWidth, mc.displayHeight, true)
        }
        return framebuffer
    }


    @JvmStatic
    fun drawRoundScale(x: Float, y: Float, width: Float, height: Float, radius: Float, color: Color, scale: Float) {
        drawRound(
            x + width - width * scale, y + height / 2f - height / 2f * scale,
            width * scale, height * scale, radius, false, color
        )
    }
    @JvmStatic
    fun drawGradientHorizontal(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        left: Color,
        right: Color
    ) {
        drawGradientRound(x, y, width, height, radius, left, left, right, right)
    }
    @JvmStatic
    fun drawGradientVertical(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        top: Color,
        bottom: Color
    ) {
        drawGradientRound(x, y, width, height, radius, bottom, top, bottom, top)
    }
    @JvmStatic
    fun drawGradientCornerLR(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        topLeft: Color,
        bottomRight: Color
    ) {
        val mixedColor = interpolateColorC(topLeft, bottomRight, .5f)
        drawGradientRound(x, y, width, height, radius, mixedColor, topLeft, bottomRight, mixedColor)
    }
    @JvmStatic
    fun drawGradientCornerRL(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        bottomLeft: Color,
        topRight: Color
    ) {
        val mixedColor = interpolateColorC(topRight, bottomLeft, .5f)
        drawGradientRound(x, y, width, height, radius, bottomLeft, mixedColor, mixedColor, topRight)
    }
    @JvmStatic
    fun drawGradientRound(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        bottomLeft: Color,
        topLeft: Color,
        bottomRight: Color,
        topRight: Color
    ) {
        GlStateManager.resetColor()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        roundedGradientShader.init()
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader)
        // Bottom Left
        roundedGradientShader.setUniformf(
            "color1",
            bottomLeft.red / 255f,
            bottomLeft.green / 255f,
            bottomLeft.blue / 255f,
            bottomLeft.alpha / 255f
        )
        //Top left
        roundedGradientShader.setUniformf(
            "color2",
            topLeft.red / 255f,
            topLeft.green / 255f,
            topLeft.blue / 255f,
            topLeft.alpha / 255f
        )
        //Bottom Right
        roundedGradientShader.setUniformf(
            "color3",
            bottomRight.red / 255f,
            bottomRight.green / 255f,
            bottomRight.blue / 255f,
            bottomRight.alpha / 255f
        )
        //Top Right
        roundedGradientShader.setUniformf(
            "color4",
            topRight.red / 255f,
            topRight.green / 255f,
            topRight.blue / 255f,
            topRight.alpha / 255f
        )
        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2)
        roundedGradientShader.unload()
        GlStateManager.disableBlend()
    }
    @JvmStatic
    fun drawRound(x: Float, y: Float, width: Float, height: Float, radius: Float, blur: Boolean, color: Color) {
        GlStateManager.resetColor()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        roundedShader.init()
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader)
        roundedShader.setUniformi("blur", if (blur) 1 else 0)
        roundedShader.setUniformf("color", color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2)
        roundedShader.unload()
        GlStateManager.disableBlend()
    }
    @JvmStatic
    fun drawRoundOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        outlineThickness: Float,
        color: Color,
        outlineColor: Color
    ) {
        GlStateManager.resetColor()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        roundedOutlineShader.init()
        val sr = ScaledResolution(Minecraft.getMinecraft())
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader)
        roundedOutlineShader.setUniformf("outlineThickness", outlineThickness * sr.scaleFactor)
        roundedOutlineShader.setUniformf(
            "color",
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )
        roundedOutlineShader.setUniformf(
            "outlineColor",
            outlineColor.red / 255f,
            outlineColor.green / 255f,
            outlineColor.blue / 255f,
            outlineColor.alpha / 255f
        )
        ShaderUtil.drawQuads(
            x - (2 + outlineThickness),
            y - (2 + outlineThickness),
            width + (4 + outlineThickness * 2),
            height + (4 + outlineThickness * 2)
        )
        roundedOutlineShader.unload()
        GlStateManager.disableBlend()
    }

    fun drawRoundTextured(x: Float, y: Float, width: Float, height: Float, radius: Float, alpha: Float) {
        GlStateManager.resetColor()
        roundedTexturedShader.init()
        roundedTexturedShader.setUniformi("textureIn", 0)
        setupRoundedRectUniforms(x, y, width, height, radius, roundedTexturedShader)
        roundedTexturedShader.setUniformf("alpha", alpha)
        ShaderUtil.drawQuads(x - 1, y - 1, width + 2, height + 2)
        roundedTexturedShader.unload()
        GlStateManager.disableBlend()
    }

    private fun setupRoundedRectUniforms(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        roundedTexturedShader: ShaderUtil
    ) {
        val sr = ScaledResolution(Minecraft.getMinecraft())
        roundedTexturedShader.setUniformf(
            "location", x * sr.scaleFactor,
            Minecraft.getMinecraft().displayHeight - height * sr.scaleFactor - y * sr.scaleFactor
        )
        roundedTexturedShader.setUniformf("rectSize", width * sr.scaleFactor, height * sr.scaleFactor)
        roundedTexturedShader.setUniformf("radius", radius * sr.scaleFactor)
    }
}