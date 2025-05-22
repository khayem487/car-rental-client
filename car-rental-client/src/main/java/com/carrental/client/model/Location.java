package com.carrental.client.model;

import java.util.Date;

public class Location {
    private Long id_location;
    private Long id_personne;
    private String mat;
    private Date dteDeb;
    private int duree;
    private String cheque;
    private String etat;  // EN_COURS, TERMINEE

    // Attributs supplémentaires pour l'affichage
    private Personne personne;
    private Voiture voiture;

    // Constructeurs
    public Location() {
    }

    public Location(Long id_location, Long id_personne, String mat, Date dteDeb, int duree, String cheque, String etat) {
        this.id_location = id_location;
        this.id_personne = id_personne;
        this.mat = mat;
        this.dteDeb = dteDeb;
        this.duree = duree;
        this.cheque = cheque;
        this.etat = etat;
    }

    // Getters et Setters
    public Long getId_location() {
        return id_location;
    }

    public void setId_location(Long id_location) {
        this.id_location = id_location;
    }

    public Long getId_personne() {
        return id_personne;
    }

    public void setId_personne(Long id_personne) {
        this.id_personne = id_personne;
    }

    public String getMat() {
        return mat;
    }

    public void setMat(String mat) {
        this.mat = mat;
    }

    public Date getDteDeb() {
        return dteDeb;
    }

    public void setDteDeb(Date dteDeb) {
        this.dteDeb = dteDeb;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public String getCheque() {
        return cheque;
    }

    public void setCheque(String cheque) {
        this.cheque = cheque;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Personne getPersonne() {
        return personne;
    }

    public void setPersonne(Personne personne) {
        this.personne = personne;
    }

    public Voiture getVoiture() {
        return voiture;
    }

    public void setVoiture(Voiture voiture) {
        this.voiture = voiture;
    }

    // Méthode pour calculer le montant total de la location
    public double getMontantTotal() {
        if (voiture != null) {
            return voiture.getPrix() * duree;
        }
        return 0;
    }

    // Méthode pour calculer la date de fin de location
    public Date getDateFin() {
        if (dteDeb != null) {
            Date dateFin = new Date(dteDeb.getTime());
            dateFin.setDate(dateFin.getDate() + duree);
            return dateFin;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id_location=" + id_location +
                ", id_personne=" + id_personne +
                ", mat='" + mat + '\'' +
                ", dteDeb=" + dteDeb +
                ", duree=" + duree +
                ", cheque='" + cheque + '\'' +
                ", etat='" + etat + '\'' +
                '}';
    }
} 