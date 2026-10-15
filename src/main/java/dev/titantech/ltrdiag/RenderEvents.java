package dev.titantech.ltrdiag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.creative.littletiles.common.block.entity.BETiles;
import java.util.LinkedHashSet;
import java.util.Set;
@Mod.EventBusSubscriber(modid="littletilesrenderdiagnostics",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class RenderEvents {
 private static ClientLevel world;
 private static final Set<BlockPos> PENDING=new LinkedHashSet<>();
 private RenderEvents() {}
 public static void changed(ClientLevel level,BlockPos pos,String oldState,String newState) {
   if(world!=level){world=level;PENDING.clear();}
   TraceLog.record("CLIENT_CHANGE "+pos+" "+oldState+" -> "+newState);
   if(PENDING.size()<1024)PENDING.add(pos.immutable());
 }
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
   if(e.phase!=TickEvent.Phase.END)return;
   ClientLevel level=Minecraft.getInstance().level;
   if(level==null||level!=world){world=level;PENDING.clear();return;}
   int count=0;
   var iterator=PENDING.iterator();
   while(iterator.hasNext()&&count++<20){
     BlockPos pos=iterator.next();iterator.remove();
     check(level,pos,"CENTER");
     for(Direction d:Direction.values())check(level,pos.relative(d),d.name());
   }
 }
 private static void check(ClientLevel level,BlockPos pos,String relation) {
   BlockEntity entity=level.getBlockEntity(pos);
   if(entity instanceof BETiles be) {
      TraceLog.record("NEIGHBOUR "+relation+" "+pos+" state="+level.getBlockState(pos)+
           " tiles="+be.tilesCount()+" boxes="+be.boxesCount()+" removed="+be.isRemoved()+
           " renderer="+(be.render==null?"NULL":be.render.getClass().getName()));
   } else {
      TraceLog.record("NEIGHBOUR "+relation+" "+pos+" state="+level.getBlockState(pos)+
           " entity="+(entity==null?"NULL":entity.getClass().getName()));
   }
 }
}
