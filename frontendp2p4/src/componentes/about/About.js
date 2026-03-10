import React from 'react';
import '../../css/stylesheet.css';
import '../../imagenes/logo.png';

const About = () => {
    return (
        <div className="about-contenedor">
            <h2 className="about-titulo">Desarrolladores</h2>
            <div className="about-nombres">
                <div className="about-nombre-item" id="about-nombre1">
                    <p>Alejandro Alvarez</p>
                </div>
                <div className="about-nombre-item" id="about-nombre2">
                    <p>Jose Alvarado</p>
                </div>
                <div className="about-nombre-item" id="about-nombre3">
                    <p>Ariana Quirós</p>
                </div>
            </div>
        </div>
    );
};

export default About;
