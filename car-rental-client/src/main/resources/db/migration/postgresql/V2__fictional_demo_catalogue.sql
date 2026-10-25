-- Seed only missing fictional catalogue rows; never overwrite bookings/profile/password changes.
INSERT INTO personne (cin, nom, prenom, numero, npermis, login, passwd, role) VALUES
('CIN001', 'Dupont', 'Jean', '0601020304', 'PERMIS001', 'jean.dupont', '$2a$12$.spnWwOTR6OaIaeGbr0dQuVdv24e/BvoMpty0SBGcg8Ri0F/JYZAe', 'CLIENT'),
('CIN002', 'Martin', 'Sophie', '0607080910', 'PERMIS002', 'sophie.martin', '$2a$12$.spnWwOTR6OaIaeGbr0dQuVdv24e/BvoMpty0SBGcg8Ri0F/JYZAe', 'CLIENT')
ON CONFLICT DO NOTHING;
INSERT INTO voiture (mat, marque, model, nbplace, disp, prix, image_path) VALUES
('AA-123-BB', 'Renault', 'Clio', 5, 'DISPONIBLE', 50, 'renault_clio.jpg'),
('CC-456-DD', 'Peugeot', '308', 5, 'DISPONIBLE', 65, 'peugeot_308.jpg'),
('EE-789-FF', 'Citroen', 'C3', 5, 'DISPONIBLE', 55, 'citroen_c3.jpg'),
('GG-012-HH', 'Volkswagen', 'Golf', 5, 'DISPONIBLE', 70, 'volkswagen_golf.jpg'),
('II-345-JJ', 'Ford', 'Focus', 5, 'PANNE', 60, 'ford_focus.jpg')
ON CONFLICT DO NOTHING;
