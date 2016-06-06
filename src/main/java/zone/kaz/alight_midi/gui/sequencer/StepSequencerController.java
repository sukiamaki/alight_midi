package zone.kaz.alight_midi.gui.sequencer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.ClassPath;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import zone.kaz.alight_midi.gui.ControllerManager;
import zone.kaz.alight_midi.inject.DIContainer;
import zone.kaz.alight_midi.sequencer.StepSequencerManager;
import zone.kaz.alight_midi.sequencer.StepSequencerPattern;

import static zone.kaz.alight_midi.gui.sequencer.StepSequencer.COLUMN_INDEX_BOX;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public class StepSequencerController implements Initializable {

    @FXML
    private Parent root;
    @FXML
    private ListView<SequencerInfo> animationList;
    @FXML
    private ListView<SequencerInfo> colorList;
    @FXML
    private ListView<SequencerInfo> patternList;
    @FXML
    private GridPane sequencerGrid;
    @FXML
    private Button addSequence;
    @FXML
    private Button removeSequence;
    @FXML
    private Label rateLabel;
    @FXML
    private Slider rateFader;
    @FXML
    private Label clockLabel;
    @FXML
    private Slider clockFader;
    @FXML
    private TextField patternNameField;
    @FXML
    private Button patternSave;

    private double colWidth = 0;

    private StepSequencerManager stepSequencerManager = DIContainer.get(StepSequencerManager.class);

    private HashMap<String, AnimationInfo> animationInfoMap = new HashMap<>();
    private HashMap<String, PatternInfo> patternInfoMap = new HashMap<>();

    public static final String CONF_DIR_PATH = System.getProperty("user.home") + "/.alight_midi";
    public static final String PATTERN_DIR_PATH = CONF_DIR_PATH + "/pattern";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: Move to Preferences.
        prepareConfDir();
        loadAnimationList();
        loadPatternList();
        ControllerManager controllerManager = DIContainer.get(ControllerManager.class);
        controllerManager.register(this);
        for (int i = 0; i < 3; i++) {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            pattern.add(new StepSequencer(
                    this,
                    i,
                    pattern.getCalcClock(),
                    pattern.getBeats(),
                    colWidth
            ));
        }
        clockFader.valueProperty().addListener(event -> {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            pattern.setClock((int) clockFader.getValue());
            updateStepSequencer(pattern);
            int clock = pattern.getCalcClock();
            int beats = pattern.getBeats();
            clockLabel.setText(String.valueOf(clock));
        });
        rateFader.valueProperty().addListener(event -> {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            pattern.setRate((int) rateFader.getValue());
            rateLabel.setText(String.valueOf(pattern.getCalcRate()));
        });
        addSequence.setOnAction(event -> {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            pattern.add(new StepSequencer(
                    this,
                    pattern.getSize(),
                    pattern.getCalcClock(),
                    pattern.getBeats(),
                    colWidth));
        });
        removeSequence.setOnAction(event -> {
            stepSequencerManager.getPattern().remove();
        });
        sequencerGrid.widthProperty().addListener((observableValue, oldValue, newValue) -> {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            double labelWidth = sequencerGrid.getColumnConstraints().get(0).getPrefWidth();
            int clock = pattern.getCalcClock();
            colWidth = (newValue.doubleValue() - labelWidth) / clock + 1;
            pattern.setButtonWidth(colWidth);
        });
        patternSave.setOnAction(event -> {
            StepSequencerPattern pattern = stepSequencerManager.getPattern();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                objectMapper.writeValue(new File(PATTERN_DIR_PATH + "/" + patternNameField.getText() + ".json"), pattern);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stepSequencerManager.getPattern().setClock(0);
        updateStepSequencer(stepSequencerManager.getPattern());
    }

    private void prepareConfDir() {
        new File(CONF_DIR_PATH).mkdir();
        new File(PATTERN_DIR_PATH).mkdir();
    }

    public double getColWidth() {
        return colWidth;
    }

    public void setRate(int rate) {
        rateFader.valueProperty().set(rate);
    }

    public void setClock(int clock) {
        clockFader.valueProperty().set(clock);
    }

    private void loadAnimationList() {
        animationList.setOnDragDetected(event -> {
            MultipleSelectionModel<SequencerInfo> items = animationList.getSelectionModel();
            SequencerInfo item = items.getSelectedItem();
            Dragboard db = animationList.startDragAndDrop(TransferMode.ANY);
            ClipboardContent content = new ClipboardContent();
            content.putString("ANIMATION:" + item);
            db.setContent(content);
            event.consume();
        });
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Set<ClassPath.ClassInfo> classInfoSet = ClassPath.from(loader).getTopLevelClasses("zone.kaz.alight_midi.sequencer.animation");
            for (ClassPath.ClassInfo classInfo : classInfoSet) {
                AnimationInfo item = new AnimationInfo(classInfo);
                animationList.itemsProperty().getValue().add(item);
                animationInfoMap.put(item.toString(), item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPatternList() {
        patternList.setOnMouseClicked(event -> {
            MultipleSelectionModel<SequencerInfo> items = patternList.getSelectionModel();
            PatternInfo item = (PatternInfo) items.getSelectedItem();
            item.loadPattern(this, stepSequencerManager);
            event.consume();
        });
        File[] patternFileList = new File(PATTERN_DIR_PATH).listFiles();
        for (File patternFile : patternFileList) {
            PatternInfo patternInfo = new PatternInfo(patternFile.getAbsolutePath());
            patternList.itemsProperty().getValue().add(patternInfo);
            patternInfoMap.put(patternInfo.toString(), patternInfo);
        }
    }

    public GridPane getSequencerGrid() {
        return sequencerGrid;
    }

    public ListView<SequencerInfo> getAnimationList() {
        return animationList;
    }

    public void updateStepSequencer(StepSequencerPattern pattern) {
        int currentClock = pattern.getCalcClock();
        ObservableList<ColumnConstraints> constraintsList = sequencerGrid.getColumnConstraints();
        if (constraintsList.size() - COLUMN_INDEX_BOX >= currentClock) {
            constraintsList.remove(currentClock, constraintsList.size() - COLUMN_INDEX_BOX);
            return;
        }
        double minWidth = 10;
        double prefWidth = 10;
        double maxWidth = Control.USE_COMPUTED_SIZE;
        for (int i = constraintsList.size() - COLUMN_INDEX_BOX; i < currentClock; i++) {
            ColumnConstraints columnConstraints = new ColumnConstraints(
                    minWidth, prefWidth, maxWidth
            );
            columnConstraints.setFillWidth(true);
            columnConstraints.setPercentWidth(-1);
            columnConstraints.setHalignment(HPos.LEFT);
            columnConstraints.setHgrow(Priority.SOMETIMES);
            if (sequencerGrid.getColumnConstraints().size() > i + COLUMN_INDEX_BOX) {
                sequencerGrid.getColumnConstraints().set(i + COLUMN_INDEX_BOX, columnConstraints);
            } else {
                sequencerGrid.getColumnConstraints().add(i + COLUMN_INDEX_BOX, columnConstraints);
            }
        }
    }

    public SequencerInfo getAnimationInfo(String key) {
        return animationInfoMap.get(key);
    }
}
