package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompletionContext;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AutoCompleteApp extends Application {
    
    private static final Lang LANG = new OtherSimpleLang();
    
    private TextArea textArea = new TextArea();
    private Label label = new Label();

    @Override
    public void start(Stage primaryStage) throws Exception {
        initCore();
        initComponents();
        HBox hbox = new HBox(5, label);
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(2));
        root.setBottom(hbox);
        root.setCenter(textArea);

        Scene scene = new Scene(root, 700, 300);
        
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
            }
        });
        
        primaryStage.setTitle("AutoComplete");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void initCore() {
        LANG.getAutoCompleter().print();
    }
    
    private void initComponents() {
        textArea.setFont(Font.font("Monaco"));
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            compile(textArea.getCaretPosition(), newValue);
        });
        textArea.caretPositionProperty().addListener((obs, oldValue, newValue) -> {
            compile(newValue.intValue(), textArea.getText());
        });
    }
    
    private void compile(int position, String text) {
        HashSet<String> candidates = new HashSet<>();
        Caret caret = Caret.from(text, position);
        boolean result = doStuff(text, candidates, caret);
        label.setText(String.format("For '%s'\n%s\n%s", text, result ? "found" : "not found", candidates.toString()));
    }
    
    public static void main(String[] args) {
        Application.launch(args);
    }
    
    private static boolean doStuff(String source, Set<String> candidates, Caret caret) {
        AutoCompletionContext<?> ctx = LANG.compile(source, caret);
        boolean result = ctx.collectCandidates();
        candidates.addAll(ctx.getCompletionCandidates());
        return result;
    }

}
