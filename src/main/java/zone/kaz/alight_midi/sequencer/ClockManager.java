package zone.kaz.alight_midi.sequencer;

import com.google.inject.Singleton;
import javafx.application.Platform;
import zone.kaz.alight_midi.gui.ControllerManager;
import zone.kaz.alight_midi.gui.main.MainController;
import zone.kaz.alight_midi.inject.DIContainer;

import java.time.Duration;
import java.time.LocalDateTime;

@Singleton
public class ClockManager extends Thread {

    ControllerManager controllerManager = DIContainer.get(ControllerManager.class);

    private boolean isPlaying = false;
    private boolean needInit = false;
    private double bpm = 135.0;
    private double realBpm = 135.0;
    private double nudgeDir;
    private LocalDateTime clock;
    private int clockInterval = 3; // ms
    private long clockCounter = 0;
    private double playTime = 0;
    private double beforePlayTime = 0;
    private long tickCounter = 0;
    private int baseTick = 480;
    private Sequencer sequencer = new Sequencer(baseTick, 4, 4);
    private boolean needPlay = false;

    // for calculating BPM
    private LocalDateTime prevTap, nowTap;
    private double dtSum;
    private double weight = 0;

    public ClockManager() {}

    public void setNeedPlay(boolean needPlay) {
        this.needPlay = needPlay;
        if (!isPlaying) {
            interrupt();
        }
    }

    public void playSequencer() {
        if (isPlaying) {
            resetSequencer();
            return;
        }
        isPlaying = true;
        needInit = true;
    }

    public void stopSequencer() {
        if (!isPlaying) {
            resetSequencer();
            initialize();
        }
        isPlaying = false;
    }

    public void resetSequencer() {
        clockCounter = 0;
        playTime = beforePlayTime = 0;
        tickCounter = 0;
        nudgeDir = 0;
        sequencer.reset();
    }

    public void tapBpm() {
        nowTap = LocalDateTime.now();
        if (prevTap == null) {
            prevTap = nowTap;
            return;
        }
        Duration duration = Duration.between(prevTap, nowTap);
        if (duration.getSeconds() > 2) {
            resetBpmCounter();
            return;
        }
        double dt = duration.getSeconds() + duration.getNano() / 1000000000.0;

        prevTap = nowTap;

        int halfSecs = 20;
        double pow = Math.pow(0.5, dt / halfSecs);
        dtSum = dtSum * pow + (1 - pow) * dt;
        weight = weight * pow + (1 - pow);
        double averageDt = dtSum / weight;
        double averageBpm = 60 / averageDt;
        setBpm(averageBpm);
        MainController mainController = (MainController) controllerManager.get(MainController.class);
        Platform.runLater(() -> mainController.setBpm(averageBpm));
    }

    private void resetBpmCounter() {
        prevTap = nowTap = null;
        dtSum = weight = 0;
    }

    public void onNudgePressed(int nudgeDir) {
        if (nudgeDir != -1 && nudgeDir != 1) {
            return;
        }
        this.nudgeDir = nudgeDir;
    }

    public void onNudgeReleased() {
        nudgeDir = 0;
        realBpm = bpm;
    }

    public void setBpm(double bpm) {
        this.bpm = realBpm = bpm;
    }

    public void initialize() {
        clock = LocalDateTime.now();
        needInit = false;
    }

    public Sequencer getSequencer() {
        return sequencer;
    }

    public void run() {
        resetSequencer();
        while (true) {
            if (needPlay) {
                playSequencer();
                needPlay = false;
            }
            if (isPlaying) {
                if (needInit) {
                    initialize();
                }

                if (nudgeDir != 0) {
                    double d = nudgeDir * 0.001;
                    realBpm += d;
                    if (realBpm <= 0 ) {
                        realBpm = 1;
                    }
                    double tickInterval = 60.0 * 1000 / realBpm / baseTick;
                    playTime = beforePlayTime + tickInterval;
                }

                double tickInterval = 60.0 * 1000 / realBpm / baseTick;
                while (clockCounter * clockInterval > playTime) {
                    tickCounter++;
                    beforePlayTime = playTime;
                    playTime += tickInterval;
                }
                sequencer.setTick(tickCounter);

                clock = clock.plus(Duration.ofMillis(clockInterval));
                LocalDateTime now = LocalDateTime.now();
                Duration duration = Duration.between(now, clock);
                clockCounter++;
                while (duration.isNegative()) {
                    duration = duration.plus(Duration.ofMillis(clockInterval));
                    clockCounter++;
                }
                long durationSecs = duration.getSeconds();
                long durationNs = duration.getNano();
                try {
                    Thread.sleep(durationSecs * 1000 + durationNs / 1000000,
                            (int) (durationNs % 1000000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // DO NOTHING
                }
            }
        }
    }

}
