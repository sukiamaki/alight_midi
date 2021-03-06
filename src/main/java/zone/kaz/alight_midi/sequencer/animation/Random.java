package zone.kaz.alight_midi.sequencer.animation;

import zone.kaz.alight_midi.device.led.Stripe;
import zone.kaz.alight_midi.sequencer.Animation;
import zone.kaz.alight_midi.sequencer.animation.util.Color;
import zone.kaz.alight_midi.sequencer.animation.util.RandomColor;

import java.util.ArrayList;
import java.util.List;

public class Random extends Animation {

    private Color color;
    private int[] positions;
    private List<Stripe> stripes;
    public Random() {
        super();
    }

    @Override
    public void init() {
        Color[] colorList = {
                new Color(0, 0xff, 0xff),
                new Color(0xff, 0, 0xff),
                new Color(0xff, 0xff, 0),
        };
        RandomColor randomColor = new RandomColor(colorList);
        this.color = randomColor.getNext();

        stripes = deviceBuffer.getStripes();
        positions = new int[stripes.size()];
        java.util.Random rand = new java.util.Random();
        int i = 0;
        for (Stripe stripe : stripes) {
            positions[i] = rand.nextInt(stripe.getBuffer().length/3);
            i++;
        }
    }

    @Override
    public void setTick(long tick) {
        int i = 0;
        long pos = tick - startTick;
        double alpha = 1;
        if (pos > tickSize / 2) {
            alpha = (double) (tickSize - pos) / (tickSize / 2);
        }
        for (Stripe stripe : stripes) {
            byte[] buffer = stripe.getBuffer();
            buffer[positions[i]*3]   = (byte) (color.getG() * alpha);
            buffer[positions[i]*3+1] = (byte) (color.getB() * alpha);
            buffer[positions[i]*3+2] = (byte) (color.getR() * alpha);
            i++;
        }
    }

}
