package musicEd.music;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;

import musicEd.reaction.Gesture;
import musicEd.reaction.Reaction;

public class Stem extends Duration implements Comparable<Stem>{
    public Staff staff;
    public Head.List heads = new Head.List();
    public boolean isUp = true;
    public Beam beam = null;

    public Stem(Staff staff, Head.List heads, boolean isUp) {
        this.staff = staff;
        this.isUp = isUp;
        this.heads = heads;
        for(Head h : heads) {h.unStem(); h.stem = this;}
        staff.sys.stems.add(this);
        setWrongSides();
        
        addReaction(new Reaction("E-E"){ // Add flag
            public int bid(Gesture gest) {
                int y = gest.vs.yM(), x1 = gest.vs.xL(), x2 = gest.vs.xH();
                int xs = Stem.this.heads.get(0).time.x;
                if(x1 > xs || x2 < xs) {return UC.noBid;}
                int y1 = Stem.this.yLo(), y2 = Stem.this.yHi();
                if(y < y1 || y > y2) {return UC.noBid;}
                int b = Math.abs(y - (y1 + y2)/2) + 55; // bias allows sys E-E to outbid this
                System.out.println("Stem E-E bids " + b);
                return b;
            }
            @Override
            public void act(Gesture gest) {
                Stem.this.incFlag();   
            }
        }); 
        addReaction(new Reaction("W-W"){ // remove flag
            public int bid(Gesture gest) {
                int y = gest.vs.yM(), x1 = gest.vs.xL(), x2 = gest.vs.xH();
                int xs = Stem.this.heads.get(0).time.x;
                if(x1 > xs || x2 < xs) {return UC.noBid;}
                int y1 = Stem.this.yLo(), y2 = Stem.this.yHi();
                if(y < y1 || y > y2) {return UC.noBid;}
                return Math.abs(y - (y1 + y2)/2) + 55; // bias allows sys W-W to outbid this
            }
            @Override
            public void act(Gesture gest) {
                Stem.this.decFlag();   
            }
        });     
    }

    @Override
    public void show(Graphics g) {
        if (nFlag >= - 1 && heads.size() > 0) {
            int x = X(), h = staff.H(), yH = yFirstHead(), yB = yBeamEnd();
            g.drawLine(x, yH, x, yB);
            if(nFlag > 0 && beam == null) { // upstem has downflags
                (isUp ? Glyph.DNFLAGS : Glyph.UPFLAGS)[nFlag - 1].showAt(g, h, x, yB);
            }
        }        
    }

    public Head firstHead() {return heads.get(isUp ? heads.size() - 1 : 0);}
    public Head lastHead() {return heads.get(isUp ? 0 : heads.size() - 1);}
    public int yFirstHead() {
        if(heads.size() == 0) {return 200;} // guard againist empty stems
        Head h = firstHead();
        return h.staff.yLine(h.line);
    }
    public int yBeamEnd() {
        if(heads.size() == 0) {return 100;} // guard againist empty stems
        if(this.beam != null && beam.stems.size() > 1 && beam.first() != this && beam.last() != this) {
            Stem b = beam.first(), e = beam.last();
            return Beam.yOfX(X(), b.X(), b.yBeamEnd(), e.X(), e.yBeamEnd());
        }
        Head h = lastHead();
        int line = h.line;
        line += isUp ? -7 : 7; // Default length of 7
        int flagInc = nFlag > 2 ? 2 * (nFlag - 2) : 0;
        line += isUp ? -flagInc : flagInc;
        if ((isUp && line > 4) || (!isUp && line < 4)) {
            line = 4;
        }
        return h.staff.yLine(line);
    }
    public int yLo() {return isUp ? yBeamEnd() : yFirstHead();}

    public int yHi() {return isUp ? yFirstHead() : yBeamEnd();}

    public int X() { 
        if(heads.size() == 0) {return 100;} // guard againist empty stems
        Head h = firstHead();
        return h.time.x + (isUp ? h.W() : 0);
    }

    public void deleteStem() {
        if(heads.size() != 0) {System.out.println("ERR_delete stems with heads on");}
        staff.sys.stems.remove(this);
        if(beam != null) {beam.removeStem(this);}
        deleteMass();
    }
    public void setWrongSides() {
        // stub
        heads.sort();
        int i, last, next;
        if(isUp){i = heads.size() - 1; last = 0; next = -1;} 
        else{i = 0; last = heads.size() - 1; next = 1;}
        Head pH = heads.get(i);
        pH.wrongSide = false;
        while(i != last) {
            i += next;
            Head nH = heads.get(i);
            nH.wrongSide = pH.staff == nH.staff && Math.abs(nH.line - pH.line) <= 1 && !pH.wrongSide;
            pH = nH;
        }
    }
    
    
    @Override
    public int compareTo(Stem s) {return X() - s.X();}

    public static Stem getStem(Staff staff, Time time, int y1, int y2, boolean up) {// factor method
        Head.List heads = new Head.List();
        for(Head h: time.heads) {
            int yH = h.Y();
            if(yH > y1 && yH < y2) {heads.add(h);}
        }
        if(heads.size() == 0) {return null;}
        Beam beam = internalStem(staff.sys, time.x, y1, y2);
        Stem res = new Stem(staff, heads, up);
        if(beam != null) {beam.addStem(res);}
        return res;
    }

    public static Beam internalStem(Sys sys, int x, int y1, int y2) {
        for(Stem s : sys.stems) {
            if(s.beam != null && s.X() < x && s.yLo() < y2 && s.yHi() > y1) {
                int bx = s.beam.first().X(), by = s.beam.first().yBeamEnd();
                int ex = s.beam.last().X(), ey = s.beam.last().yBeamEnd();
                if(Beam.verticalLineCrossesSegment(x, y1, y2, bx, by, ex, ey)) {return s.beam;}
            }
        }
        return null;
    }
    //-------------------------list--------------------------
    public static class List extends ArrayList<Stem>{
        public void sort() {Collections.sort(this);}
        public Stem.List allIntersectors(int x1, int y1, int x2, int y2) {
            Stem.List res = new Stem.List();
            for(Stem s : this) {
                int x = s.X(), y = Beam.yOfX(x, x1, y1, x2, y2);
                if(x > x1 && x < x2 && y > s.yLo() && y < s.yHi()) {res.add(s);}
            }
            return res;
        }
    }

    
}
