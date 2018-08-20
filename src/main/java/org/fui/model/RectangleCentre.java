package org.fui.model;


import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;

public class RectangleCentre {
    private int page;
    private float centreX;
    private float centreY;

    public RectangleCentre() {
        super();
    }

    public RectangleCentre(int page) {
        super();
        this.page = page;
    }

    public RectangleCentre copy() {
        return new RectangleCentre(this.page);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public float getCentreX() {
        return centreX;
    }

    public void setCentreX(float centreX) {
        this.centreX = centreX;
    }

    public float getCentreY() {
        return centreY;
    }

    public void setCentreY(float centreY) {
        this.centreY = centreY;
    }

    @Override
    public String toString() {
        return "RectangleText [page=" + page + ", centreX=" + centreX + ", centreY=" + centreY + "]";
    }

    public Rectangle getRectangle(Image signImage) {
        float halfWidth = signImage.getWidth() / 10;
        float halfHeight = signImage.getHeight() / 10;
        return new Rectangle(this.centreX - halfWidth, this.centreY - halfHeight, this.centreX + halfWidth, this.centreY + halfHeight);
    }
}
