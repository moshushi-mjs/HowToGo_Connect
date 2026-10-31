package io.github.moshushimjs.htgconnect.mixin;

import java.nio.file.Path;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import bili.dongsz.howtogo.client.MapDataScreen;
import bili.dongsz.howtogo.client.UiTheme;
import io.github.moshushimjs.htgconnect.client.ClientRoads;
import io.github.moshushimjs.htgconnect.client.ServerCatalog;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(MapDataScreen.class)
public abstract class MapDataScreenMixin {

    private static final int PAD = 10;
    private static final int GAP = 6;
    private static final int BUTTON_HEIGHT = 18;
    private static final int PANEL_MAX_HEIGHT = 300;
    private static final int LINE_HEIGHT = 11;
    private static final int CLOSE_WIDTH = 80;
    private static final int CLOSE_HEIGHT = 16;
    private static final int CARD_MARGIN = 8;
    private static final int GREY_PERCENT = 80;
    private static final int DARK_BAND = 0x1AFFFFFF;
    private static final int HOVER_RED = 0xFFE02020;

    @Shadow
    private int panelX;

    @Shadow
    private int panelY;

    @Shadow
    private int panelW;

    @Shadow
    private int panelH;

    @Shadow
    private int listX;

    @Shadow
    private int listW;

    @Shadow
    private int listY;

    @Shadow
    private int listH;

    private boolean htgShown;
    private int htgExportX;
    private int htgExportY;
    private int htgExportW;
    private int htgBandLeft;
    private int htgBandTop;
    private int htgBandRight;
    private int htgBandBottom;
    private int htgCardX;
    private int htgCardY;
    private int htgCardW;
    private int htgCardH;
    private int htgCloseX;
    private int htgCloseY;

    @Inject(method = "init", at = @At("TAIL"))
    private void htg$init(CallbackInfo ci) {
        htg$layout();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void htg$dim(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!this.htgShown) {
            return;
        }
        Screen self = (Screen) (Object) this;
        graphics.fill(0, 0, self.width, self.height, UiTheme.dim());
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void htg$greySheet(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!this.htgShown) {
            return;
        }
        int outerLeft = this.panelX - 2;
        int outerTop = this.panelY - 2;
        int outerRight = this.panelX + this.panelW + 2;
        int outerBottom = this.panelY + this.panelH + 2;
        int edge = UiTheme.windowEdge();
        graphics.fill(outerLeft, outerTop, outerRight, this.panelY, edge);
        graphics.fill(outerLeft, this.panelY + this.panelH, outerRight, outerBottom, edge);
        graphics.fill(outerLeft, outerTop, this.panelX, outerBottom, edge);
        graphics.fill(this.panelX + this.panelW, outerTop, outerRight, outerBottom, edge);

        graphics.fill(this.htgBandLeft, this.htgBandTop, this.htgBandRight, this.htgBandBottom,
                htg$bandColour());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void htg$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!this.htgShown) {
            return;
        }
        graphics.fill(this.listX, this.listY, this.listX + this.listW, this.listY + this.listH,
                htg$bandColour());
        htg$drawCard(graphics, mouseX, mouseY);
        htg$drawDisabledExport(graphics);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void htg$click(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (!this.htgShown || button != 0) {
            return;
        }
        if (htg$inside(mouseX, mouseY, this.htgExportX, this.htgExportY, this.htgExportW,
                BUTTON_HEIGHT)) {
            cir.setReturnValue(true);
            return;
        }
        if (htg$inside(mouseX, mouseY, this.htgCloseX, this.htgCloseY, CLOSE_WIDTH,
                CLOSE_HEIGHT)) {
            if (htg$canStop()) {
                ClientRoads.decline();
                htg$layout();
            }
            cir.setReturnValue(true);
            return;
        }
        if (mouseX >= this.htgBandLeft && mouseX < this.htgBandRight
                && mouseY >= this.htgBandTop && mouseY < this.htgBandBottom) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onFilesDrop", at = @At("HEAD"), cancellable = true)
    private void htg$drop(List<Path> paths, CallbackInfo ci) {
        if (this.htgShown) {
            ci.cancel();
        }
    }

    @Inject(method = "rowAt", at = @At("HEAD"), cancellable = true)
    private void htg$noRow(double mouseX, double mouseY, CallbackInfoReturnable<Integer> cir) {
        if (this.htgShown) {
            cir.setReturnValue(-1);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void htg$scroll(double mouseX, double mouseY, double scrollX, double scrollY,
            CallbackInfoReturnable<Boolean> cir) {
        if (this.htgShown) {
            cir.setReturnValue(true);
        }
    }

    private void htg$layout() {
        this.htgShown = false;
        try {
            if (!ClientRoads.consented()) {
                return;
            }
            Screen self = (Screen) (Object) this;
            int original = Math.min(PANEL_MAX_HEIGHT, self.height - 16);

            int row2Y = this.panelY + original - PAD - BUTTON_HEIGHT;
            int row1Y = row2Y - BUTTON_HEIGHT - GAP;

            this.htgExportX = this.panelX + PAD;
            this.htgExportY = row1Y;
            this.htgExportW = (this.panelW - PAD * 2 - GAP) / 2;

            this.htgBandLeft = this.panelX;
            this.htgBandTop = this.panelY + PAD + LINE_HEIGHT;
            this.htgBandRight = this.panelX + this.panelW;
            this.htgBandBottom = row1Y - 4;

            int cardW = this.panelW - PAD * 2 - CARD_MARGIN * 2;
            int cardH = LINE_HEIGHT * 2 + 8 + CLOSE_HEIGHT + 12;
            int bandH = this.htgBandBottom - this.htgBandTop;
            this.htgCardW = cardW;
            this.htgCardH = cardH;
            this.htgCardX = this.htgBandLeft + (this.htgBandRight - this.htgBandLeft - cardW) / 2;
            this.htgCardY = this.htgBandTop + Math.max(4, (bandH - cardH) / 2);

            this.htgCloseX = this.htgCardX + (cardW - CLOSE_WIDTH) / 2;
            this.htgCloseY = this.htgCardY + cardH - CLOSE_HEIGHT - 6;
            this.htgShown = true;
        } catch (Throwable ignored) {
            this.htgShown = false;
        }
    }

    private void htg$drawCard(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(this.htgCardX - 1, this.htgCardY - 1, this.htgCardX + this.htgCardW + 1,
                this.htgCardY + this.htgCardH + 1, htg$edgeColour());
        graphics.fill(this.htgCardX, this.htgCardY, this.htgCardX + this.htgCardW,
                this.htgCardY + this.htgCardH, htg$cardColour());

        Font font = ((Screen) (Object) this).getMinecraft().font;
        Component first = Component.translatable("htg_connect.mapdata.disabled.first");
        Component second = Component.translatable("htg_connect.mapdata.disabled.second");
        graphics.drawString(font, first, this.htgCardX + (this.htgCardW - font.width(first)) / 2,
                this.htgCardY + 8, htg$textColour(), false);
        graphics.drawString(font, second, this.htgCardX + (this.htgCardW - font.width(second)) / 2,
                this.htgCardY + 8 + LINE_HEIGHT, htg$textSoftColour(), false);

        boolean canStop = htg$canStop();
        boolean hovered = htg$inside(mouseX, mouseY, this.htgCloseX, this.htgCloseY, CLOSE_WIDTH,
                CLOSE_HEIGHT);
        int face = !canStop ? htg$scale(htg$buttonColour(), 92)
                : (hovered ? HOVER_RED : UiTheme.danger());
        graphics.fill(this.htgCloseX, this.htgCloseY, this.htgCloseX + CLOSE_WIDTH,
                this.htgCloseY + CLOSE_HEIGHT, face);
        graphics.fill(this.htgCloseX, this.htgCloseY, this.htgCloseX + CLOSE_WIDTH,
                this.htgCloseY + 1, htg$edgeColour());

        Component label = Component.translatable("htg_connect.mapdata.disabled.button");
        String shown = font.plainSubstrByWidth(label.getString(), CLOSE_WIDTH - 6);
        graphics.drawString(font, shown, this.htgCloseX + (CLOSE_WIDTH - font.width(shown)) / 2,
                this.htgCloseY + (CLOSE_HEIGHT - 8) / 2,
                canStop ? UiTheme.onSelection() : htg$textSoftColour(), false);

        if (!canStop && hovered) {
            Component note = Component.translatable("htg_connect.mapdata.disabled.unavailable");
            int noteWidth = font.width(note);
            int noteY = this.htgCloseY - 14;
            int boxLeft = this.htgCloseX + CLOSE_WIDTH / 2 - noteWidth / 2 - 3;
            int boxRight = this.htgCloseX + CLOSE_WIDTH / 2 + noteWidth / 2 + 3;
            int boxTop = noteY - 3;
            int boxBottom = noteY + 11;
            graphics.fill(boxLeft, boxTop, boxRight, boxBottom, UiTheme.panelSolid());
            graphics.fill(boxLeft, boxTop, boxRight, boxTop + 1, UiTheme.panelEdge());
            graphics.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, UiTheme.panelEdge());
            graphics.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, UiTheme.panelEdge());
            graphics.fill(boxRight - 1, boxTop, boxRight, boxBottom, UiTheme.panelEdge());
            graphics.drawString(font, note,
                    this.htgCloseX + CLOSE_WIDTH / 2 - noteWidth / 2, noteY,
                    UiTheme.textSecondary(), false);
        }
    }

    private void htg$drawDisabledExport(GuiGraphics graphics) {
        int right = this.htgExportX + this.htgExportW;
        int bottom = this.htgExportY + BUTTON_HEIGHT;
        graphics.fill(this.htgExportX, this.htgExportY, right, bottom,
                htg$scale(UiTheme.button(), 86));

        Font font = ((Screen) (Object) this).getMinecraft().font;
        Component label = Component.translatable("screen.howtogo.mapdata.export");
        String shown = font.plainSubstrByWidth(label.getString(), this.htgExportW - 6);
        graphics.drawString(font, shown,
                this.htgExportX + (this.htgExportW - font.width(shown)) / 2,
                this.htgExportY + (BUTTON_HEIGHT - 8) / 2, UiTheme.textMuted(), false);
    }

    private static boolean htg$inside(double mouseX, double mouseY, int x, int y, int width,
            int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static boolean htg$canStop() {
        return !ServerCatalog.kickOnDecline();
    }

    private static boolean htg$dark() {
        return !UiTheme.light();
    }

    private static int htg$bandColour() {
        return htg$dark() ? DARK_BAND : htg$scale(UiTheme.panelSolid(), GREY_PERCENT);
    }

    private static int htg$cardColour() {
        return UiTheme.panelSolid();
    }

    private static int htg$edgeColour() {
        return UiTheme.panelEdge();
    }

    private static int htg$textColour() {
        return UiTheme.textStrong();
    }

    private static int htg$textSoftColour() {
        return UiTheme.textSecondary();
    }

    private static int htg$buttonColour() {
        return UiTheme.button();
    }

    private static int htg$buttonHover() {
        return UiTheme.buttonHover();
    }
    private static int htg$scale(int argb, int percent) {
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >> 16) & 0xFF) * percent / 100.0);
        int g = (int) (((argb >> 8) & 0xFF) * percent / 100.0);
        int b = (int) ((argb & 0xFF) * percent / 100.0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
