package com.albatarm.autocomplete.app;

import java.util.Set;
import java.util.stream.Collectors;

import com.albatarm.autocomplete.CompletionProposal;

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
    
    private static final Lang LANG = new CalcLang();
    
    private TextArea textArea = new TextArea();
    private Label label = new Label();

    @Override
    public void start(Stage primaryStage) throws Exception {
        initCore();
        initComponents();
        label.setMinHeight(150);
        label.setWrapText(true);
        HBox hbox = new HBox(5, label);
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(2));
        root.setBottom(hbox);
        root.setCenter(textArea);

        Scene scene = new Scene(root, 700, 500);
        
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
        Caret caret = Caret.from(text, position);
        CompletionProposal proposal = LANG.compile2(text, caret);
        
        Set<String> reducted = reduct(proposal.getCandidates(), proposal.getCurrentToken());
        
        label.setText(String.format("For '%s'\n%s\n%s\n%s\n%s", 
        		text, 
        		proposal.isFullyParsed() ? "fully parsed" : "not fully parsed", 
        		proposal.getCurrentToken(), 
        		proposal.getCandidates(),
        		reducted
        ));
    }
    
    private Set<String> reduct(Set<String> candidates, String start) {
    	return candidates.stream().filter(token -> {
    		if (token.startsWith("'")) {
    			return token.startsWith("'" + start);
    		} else {
    			return true;
    		}
    	}).collect(Collectors.toSet());
    }
    
    public static void main(String[] args) {
        Application.launch(args);
    }
    
}
