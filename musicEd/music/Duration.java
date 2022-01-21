package musicEd.music;

import musicEd.reaction.Mass;

import java.awt.Graphics;

public abstract class Duration extends Mass { // extends Mass in order to have the subclass take in the Mass properties. Duration itself won't show
    public int nFlag = 0, nDot = 0;

    public Duration() {super("NOTE");}

    public abstract void show(Graphics g); // abstract method. This is not necessary and just emphasize show has not been implemented.
    public void incFlag() {if (nFlag < 4) {nFlag++;}}
    public void decFlag() {if (nFlag > -2) {nFlag--;}}
    public void cycleDot() {nDot++; if (nDot > 3) {nDot = 0;}}

}
