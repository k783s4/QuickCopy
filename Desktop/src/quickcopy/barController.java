/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quickcopy;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;


/**
 *
 * @author Chipleo
 */
public class barController implements Initializable {

    PackageManagerController contr;
    bar na;
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    @FXML
    private void open(MouseEvent event) {
        contr.open(na);
    }
    
    public void send(PackageManagerController _contr, bar our){
        contr = _contr;
        na = our;
    }

}
