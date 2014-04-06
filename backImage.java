/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.alljoyn.bus.sample.chat;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;

/**
 *
 * @author Himanshu
 */
public class backImage extends JComponent {
 
Image i;
 
//Creating Constructer
public backImage(Image i) {
this.i = i;
 
}
 
//Overriding the paintComponent method
@Override
public void paintComponent(Graphics g) {
 
g.drawImage(i, 0, 0, null);  // Drawing image using drawImage method
 
}
}
