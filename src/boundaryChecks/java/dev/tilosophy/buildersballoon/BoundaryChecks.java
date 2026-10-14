package dev.tilosophy.buildersballoon;
import java.util.Random;
/** Recreated deterministic geometry checks, independent of a game launch. */
public final class BoundaryChecks {
    public static void main(String[] args) {
        var random=new Random(217611);int checks=0;
        for(int range:new int[]{8,16,32,48,64}) {
            var b=new TetherBoundary(1.5,70.5,-99.5,range);
            for(int i=0;i<20000;i++) {
                double x=b.x()+(random.nextDouble()*2-1)*range;
                double y=b.y()+(random.nextDouble()*2-1)*range;
                double z=b.z()+(random.nextDouble()*2-1)*range;
                double dx=(random.nextDouble()*2-1)*range*5;
                double dy=(random.nextDouble()*2-1)*range*5;
                double dz=(random.nextDouble()*2-1)*range*5;
                double cx=b.movementX(x,dx),cy=b.movementY(y,dy),cz=b.movementZ(z,dz);
                if(Math.abs(x+cx-b.x())>range+1E-9 || Math.abs(y+cy-b.y())>range+1E-9 || Math.abs(z+cz-b.z())>range+1E-9
                    || Math.abs(cx)>Math.abs(dx)+1E-9 || Math.abs(cy)>Math.abs(dy)+1E-9 || Math.abs(cz)>Math.abs(dz)+1E-9) throw new AssertionError("Unsafe movement");
                if(b.contains(x+dx,y+dy,z+dz) && (cx!=dx||cy!=dy||cz!=dz)) throw new AssertionError("Accepted movement changed");
                checks++;
            }
            if(b.movementX(b.x()+range,1)!=0 || b.movementX(b.x()+range,-1)!=-1) throw new AssertionError("Face direction");checks++;
            if(!Double.isNaN(b.clampY(Double.NaN)) || b.clampX(Double.POSITIVE_INFINITY)!=Double.POSITIVE_INFINITY) throw new AssertionError("Invalid packet values hidden");checks++;
        }
        for (int invalid : new int[]{7,65,Integer.MIN_VALUE,Integer.MAX_VALUE}) {
            try { new TetherBoundary(0,0,0,invalid);throw new AssertionError("Invalid range accepted: "+invalid); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        System.out.println("Recreated boundary checks passed: "+checks);
        FrameChecks.run();
    }
}
