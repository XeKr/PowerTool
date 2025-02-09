package org.teacon.powertool.client.renders.holo_sign;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.ClientCommandSourceStack;
import org.teacon.powertool.block.entity.RawJsonHolographicSignBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class RawJsonHolographicSignBlockEntityRenderer implements BlockEntityRenderer<RawJsonHolographicSignBlockEntity> {
    
    private final BlockEntityRenderDispatcher dispatcher;
    private final Font font;
    
    public RawJsonHolographicSignBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.dispatcher = context.getBlockEntityRenderDispatcher();
    }
    
    @Override
    public void render(RawJsonHolographicSignBlockEntity theSign, float partialTick, PoseStack transform, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        var renderHoverText = Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getBlockPos().equals(theSign.getBlockPos());
        renderInternal(theSign, transform, bufferSource, packedLight, theSign.rotate,renderHoverText);
        if (theSign.bidirectional) {
            renderInternal(theSign, transform, bufferSource, packedLight, (theSign.rotate + 180) % 360,renderHoverText);
        }
    }
    
    public void renderComponent(Component component,float x,float y,PoseStack transform,MultiBufferSource bufferSource, int packedLight,int fontColorDefault,int bgColor) {
        if(component.equals(Component.empty()) || component.getString().isEmpty()) return;
        var textColor = component.getStyle().getColor();
        int fontColor = textColor == null ? fontColorDefault : textColor.getValue();
        int w = this.font.width(component);
        // FIXME Implement all 3 different shadow types
        this.font.drawInBatch(component,x- (float) w /2,y,fontColor,false,transform.last().pose(),bufferSource, Font.DisplayMode.NORMAL,bgColor,packedLight);
    }
    
    //todo 应用对齐方式
    public float renderComponentList(List<Component> components, float x, float y, PoseStack transform, MultiBufferSource bufferSource, int packedLight, int fontColorDefault, int bgColor,boolean renderHoverText) {
        var yr = y;
        for (var component : components) {
            renderComponent(component, x, y, transform, bufferSource, packedLight, fontColorDefault, bgColor);
            y += this.font.lineHeight+1;
            if (renderHoverText) {
                y += renderHoverText(component, x, y, transform, bufferSource, packedLight, bgColor);
            }
        }
        return y-yr;
    }
    
    public float renderHoverText(Component component,float x,float y,PoseStack transform, MultiBufferSource bufferSource, int packedLight, int bgColor){
        var yr = y;
        var hoverEvent = component.getStyle().getHoverEvent();
        if (hoverEvent != null){
            var action = hoverEvent.getAction();
            if(action == HoverEvent.Action.SHOW_TEXT){
                var text = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
                if(text != null){
                    renderComponent(text,x,y,transform,bufferSource,packedLight,0xffffff,bgColor);
                    y+=this.font.lineHeight+1;
                }
            }
            if(action == HoverEvent.Action.SHOW_ENTITY){
                var entity_info = hoverEvent.getValue(HoverEvent.Action.SHOW_ENTITY);
                if(entity_info != null){
                    y+=renderComponentList(entity_info.getTooltipLines(),x,y,transform,bufferSource,packedLight,0xffffff,bgColor,true);
                }
            }
            if(action == HoverEvent.Action.SHOW_ITEM){
                var item_info = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
                if(item_info != null){
                    y+=renderComponentList(Screen.getTooltipFromItem(Minecraft.getInstance(),item_info.getItemStack()),x,y,transform,bufferSource,packedLight,0xffffff,bgColor,true);
                }
            }
        }
        return yr-y;
    }
    
    public void renderInternal(RawJsonHolographicSignBlockEntity theSign, PoseStack transform, MultiBufferSource bufferSource, int packedLight, int rotatedDegree,boolean renderHoverText) {
        transform.pushPose();
        HolographicSignBlockEntityRenderer.beforeRender(theSign, transform, dispatcher, rotatedDegree);
        int yOffset = (int) -(0.5 * this.font.lineHeight);
        renderComponentList(theSign.forRender,0,yOffset,transform,bufferSource,packedLight,theSign.colorInARGB,theSign.bgColorInARGB,renderHoverText);
        transform.popPose();
    }
}
