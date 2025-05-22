package com.carrental.client.model;

public class Personne {
    private Long id;
    private String cin;
    private String nom;
    private String prenom;
    private String numero;
    private String npermis;
    private int nbloc;
    private String login;
    private String passwd;
    private String role;

    // Constructeurs
    public Personne() {
    }

    public Personne(Long id, String cin, String nom, String prenom, String numero, String npermis, int nbloc, String login, String passwd, String role) {
        this.id = id;
        this.cin = cin;
        this.nom = nom;
        this.prenom = prenom;
        this.numero = numero;
        this.npermis = npermis;
        this.nbloc = nbloc;
        this.login = login;
        this.passwd = passwd;
        this.role = role;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCin() {
        return cin;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNpermis() {
        return npermis;
    }

    public void setNpermis(String npermis) {
        this.npermis = npermis;
    }

    public int getNbloc() {
        return nbloc;
    }

    public void setNbloc(int nbloc) {
        this.nbloc = nbloc;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Personne{" +
                "id=" + id +
                ", cin='" + cin + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", numero='" + numero + '\'' +
                ", npermis='" + npermis + '\'' +
                ", nbloc=" + nbloc +
                ", login='" + login + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
} 