package com.raeids.stratus

import com.raeids.stratus.command.StratusCommand
import com.raeids.stratus.config.StratusConfig
import com.raeids.stratus.hook.ChatShortcuts
import com.raeids.stratus.hook.ChatTabs
import com.raeids.stratus.hook.GuiNewChatHook
import com.raeids.stratus.hook.filterMessages
import com.raeids.stratus.mixin.GuiNewChatAccessor
import com.raeids.stratus.updater.Updater
import com.raeids.stratus.utils.RenderHelper
import gg.essential.api.EssentialAPI
import gg.essential.universal.UDesktop
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.*
import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.shader.Framebuffer
import net.minecraft.util.MathHelper
import net.minecraftforge.common.MinecraftForge.EVENT_BUS
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@Mod(
    modid = Stratus.ID,
    name = Stratus.NAME,
    version = Stratus.VER,
    modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter"
)
object Stratus {

    val keybind = KeyBinding("Screenshot Chat", Keyboard.KEY_NONE, "Stratus")
    const val NAME = "@NAME@"
    const val VER = "@VER@"
    const val ID = "@ID@"
    var doTheThing = false
    lateinit var jarFile: File
        private set
    var isPatcher = false
    private set
    var isBetterChat = false
    private set

    private val fileFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss'.png'")

    val modDir = File(File(Minecraft.getMinecraft().mcDataDir, "W-OVERFLOW"), NAME)

    @Mod.EventHandler
    fun onFMLPreInitialization(event: FMLPreInitializationEvent) {
        if (!modDir.exists()) modDir.mkdirs()
        jarFile = event.sourceFile
    }

    @Mod.EventHandler
    fun onInitialization(event: FMLInitializationEvent) {
        StratusConfig.preload()
        StratusCommand.register()
        ClientRegistry.registerKeyBinding(keybind)
        EVENT_BUS.register(this)
        ChatTabs.initialize()
        ChatShortcuts.initialize()
        Updater.update()
    }

    @Mod.EventHandler
    fun onPostInitialization(event: FMLPostInitializationEvent) {
        isPatcher = Loader.isModLoaded("patcher")
        isBetterChat = Loader.isModLoaded("betterchat")
    }

    @SubscribeEvent
    fun onTickEvent(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START && Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().thePlayer != null && (Minecraft.getMinecraft().currentScreen == null || Minecraft.getMinecraft().currentScreen is GuiChat)) {
            if (doTheThing) {
                screenshotChat()
                doTheThing = false
            }
        }
    }

    fun screenshotLine(line: ChatLine): BufferedImage? {
        val hud = Minecraft.getMinecraft().ingameGUI
        val chat = hud.chatGUI
        val i = MathHelper.floor_float(chat.chatWidth/chat.chatScale)
        return screenshot(GuiUtilRenderComponents.splitText(line.chatComponent, i, Minecraft.getMinecraft().fontRendererObj, false, false).map { it.formattedText }.reversed(), chat.chatWidth)
    }

    private fun screenshotChat() {
        screenshotChat(0)
    }

    fun screenshotChat(scrollPos: Int) {
        val hud = Minecraft.getMinecraft().ingameGUI
        val chat = hud.chatGUI
        val chatLines = ArrayList<String>()
        filterMessages((chat as GuiNewChatHook).prevText, (chat as GuiNewChatAccessor).drawnChatLines)?.let { drawnLines ->
            for (i in scrollPos until drawnLines.size.coerceAtMost(scrollPos+GuiNewChat.calculateChatboxHeight(Minecraft.getMinecraft().gameSettings.chatHeightFocused)/9)){
                chatLines.add(drawnLines[i].chatComponent.formattedText)
            }

            screenshot(chatLines, chat.chatWidth)?.let {
                RenderHelper.copyBufferedImageToClipboard(it)
            }
        }
    }

    private fun screenshot(messages: List<String>, width: Int): BufferedImage? {
        if (messages.isEmpty()) {
            EssentialAPI.getNotifications().push("Stratus", "Chat window is empty.")
            return null
        }

        val fr: FontRenderer = Minecraft.getMinecraft().fontRendererObj
        val fb: Framebuffer = RenderHelper.createBindFramebuffer(width, messages.size*9)
        val file = File(Minecraft.getMinecraft().mcDataDir, "screenshots/chat/" + fileFormatter.format(Date()))

        for (i in messages.indices) fr.drawStringWithShadow(messages[i], 0f, (messages.size-1-i)*9f, 0xffffff)

        val image = RenderHelper.screenshotFramebuffer(fb, file)
        Minecraft.getMinecraft().entityRenderer.setupOverlayRendering()
        Minecraft.getMinecraft().framebuffer.bindFramebuffer(true)
        EssentialAPI.getNotifications()
            .push("Stratus", "Chat screenshotted successfully.\nClick to open.") {
                if (!UDesktop.browse(file.toURI())) {
                    EssentialAPI.getNotifications().push("Stratus", "Could not browse!")
                }
            }
        return image
    }
}
