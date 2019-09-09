/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quickcopy;

import java.io.IOException;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

/**
 *
 * @author Chipleo
 */
public class open_bar {
    Pane bar;
        
    public open_bar(Package pack, PackageManagerController contr){
        
        //load open_bar FXML design
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("bar_opened.fxml"));
        try{ 
        bar = (Pane) loader.load();
        openbarController b = loader.getController();
        b.send(contr,this);
        //set all settings and layout options
        List<Connection> rec = pack.getRecipients();
        String recipients_formatted = "TO:";
        for(Connection c : rec){
            recipients_formatted =(new StringBuilder()).append(recipients_formatted).append(c.getName() + ",").toString();
        }
        b.setAll(pack.getDate(), pack.getName(), recipients_formatted);
        }catch(IOException e){
            System.out.println("Could not load open_bar out of FXML,: " + e.toString());
        }
    }
    
    public Pane getBar(){
        return bar;
    }
}