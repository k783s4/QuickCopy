/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quickcopy;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 *
 * @author Chipleo
 */
public class PackageManagerController implements Initializable {

    @FXML
    ScrollPane scrollpane;
    @FXML
    Pane pane;
    @FXML
    private Button bar_add, bar_add_large;
    @FXML
    private VBox list;
    private List<Package> packages = new ArrayList<>();
    private List<bar> bars = new ArrayList<>();
    private List<open_bar> open_bars = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    @FXML
    private void draw() {
        //test for boxes, should get them from memory
        List<String> fl = new ArrayList<>();
        fl.add("file1");
        fl.add("file2");
        List<Connection> co = new ArrayList<>();
        co.add(new Connection("192.168.2.103", 22));

        //add all packages to package list
        int z = 0;
        for (Package box : packages) {
            //create new bar using package "box"
            bars.add(new bar(box, this));
            //add to list
            list.getChildren().add(bars.get(bars.size() - 1).getBar());
            //create open_bar
            open_bars.add(new open_bar(box, this));
            z++;
        }
    }

    @FXML
    private void addPackage() {
        //add empty package from button
        //get date and time in new format
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy / HH:mm");
        Date date = new Date();
        //create new empty package
        Package i = new Package("Empty", formatter.format(date));
        //add to package list
        packages.add(i);
        //create a bar
        bars.add(new bar(i, this));
        //render bar
        list.getChildren().add(bars.get(bars.size() - 1).getBar());
        //create an open_bar
        open_bars.add(new open_bar(i, this));
        System.out.println(open_bars.get(open_bars.size() - 1).getBar().getHeight());
        //prolongue scroll height
        //scrollpane.setPrefHeight(scrollpane.getHeight() + 55 + 8);
        refresh();
    }

    public void sendScene(ScrollPane scene) {
        //list = (VBox) scene.lookup("#package_list");
        assert (null != list);
        draw();
    }

    public void open(bar me) {
        //replace closed bar with opened bar

        //get bar to replace
        int index = bars.indexOf(me);
        //set open_bar
        list.getChildren().set(index, open_bars.get(index).getBar());

        //extend scrollbar
        refresh();
    }

    public void close(open_bar me) {
        //replace open bar with closed bar
        //get bar to replace
        int index = open_bars.indexOf(me);
        //set it
        list.getChildren().set(index, bars.get(index).getBar());

        //retract scrollbar
        refresh();
    }

    public void delete(open_bar me) {
        //get bar to delete
        int index = open_bars.indexOf(me);

        //delete it
        refresh();
        list.getChildren().remove(index);
        bars.remove(index);
        open_bars.remove(index);

    }

    public void delete(bar me) {
        //get bar to delete
        int index = bars.indexOf(me);
        //delete it
        refresh();
        list.getChildren().remove(index);
        bars.remove(index);
        open_bars.remove(index);
    }

    public void refresh(open_bar na) {
        list.getChildren().set(open_bars.indexOf(na), na.getBar());
        //set appropriate height
        pane.setPrefHeight(list.getHeight() + 500);
    }

    void refresh() {
        //set appropriate height
        pane.setPrefHeight(list.getHeight() + 500);

    }
}
