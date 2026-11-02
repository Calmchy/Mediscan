/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Frontend;

import javax.swing.ImageIcon;

/**
 *
 * @author Calmchy
 */
public class AppIcon {
    private static final ImageIcon ICON = new ImageIcon(AppIcon.class.getResource("/Images/logo-64.png"));

    public static ImageIcon getIcon() {
        return ICON;
    }
}
