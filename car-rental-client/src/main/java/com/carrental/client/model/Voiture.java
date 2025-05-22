package com.carrental.client.model;

public class Voiture {
    private String mat;
    private String marque;
    private String model;
    private int nbplace;
    private String disp;  // DISPONIBLE, LOUEE, PANNE
    private int nbloc;
    private double prix;
    private String imagePath;

    // Constructeurs
    public Voiture() {
    }

    public Voiture(String mat, String marque, String model, int nbplace, String disp, int nbloc, double prix) {
        this.mat = mat;
        this.marque = marque;
        this.model = model;
        this.nbplace = nbplace;
        this.disp = disp;
        this.nbloc = nbloc;
        this.prix = prix;
        this.imagePath = "default.jpg";
    }
    
    public Voiture(String mat, String marque, String model, int nbplace, String disp, int nbloc, double prix, String imagePath) {
        this.mat = mat;
        this.marque = marque;
        this.model = model;
        this.nbplace = nbplace;
        this.disp = disp;
        this.nbloc = nbloc;
        this.prix = prix;
        this.imagePath = imagePath;
    }

    // Getters et Setters
    public String getMat() {
        return mat;
    }

    public void setMat(String mat) {
        this.mat = mat;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getNbplace() {
        return nbplace;
    }

    public void setNbplace(int nbplace) {
        this.nbplace = nbplace;
    }

    public String getDisp() {
        return disp;
    }

    public void setDisp(String disp) {
        this.disp = disp;
    }

    public int getNbloc() {
        return nbloc;
    }

    public void setNbloc(int nbloc) {
        this.nbloc = nbloc;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }
    
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return "Voiture{" +
                "mat='" + mat + '\'' +
                ", marque='" + marque + '\'' +
                ", model='" + model + '\'' +
                ", nbplace=" + nbplace +
                ", disp='" + disp + '\'' +
                ", nbloc=" + nbloc +
                ", prix=" + prix +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
} 