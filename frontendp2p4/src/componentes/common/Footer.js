import React from 'react';
import '../../css/common.css';
import telefono from '../../imagenes/telefono.png';

const Footer = () => {
    return (
        <footer>
            <div className="footer-container">
                <span id="foot">Total Soft Inc.</span>
                <div className="contact-info">
                    <img src={telefono + ''} alt="Contact"/>
                    <i>+506 5467 0937</i>
                </div>
                <div className="social-icons">
                    <a href="https://twitter.com" target="_blank" rel="noreferrer"><i className="bi bi-twitter"></i></a>
                    <a href="https://facebook.com" target="_blank" rel="noreferrer"><i
                        className="bi bi-facebook"></i></a>
                    <a href="https://instagram.com" target="_blank" rel="noreferrer"><i className="bi bi-instagram"></i></a>
                </div>
            </div>
            <p style={{marginRight: '15px'}}>© 2019 Tsf, Inc.</p>
        </footer>
    );
};

export default Footer;
