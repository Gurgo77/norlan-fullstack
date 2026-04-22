import { json } from '@sveltejs/kit';
import nodemailer from 'nodemailer';
// Importazione sicura delle credenziali dal file .env
import { EMAIL_USER, EMAIL_PASS } from '$env/static/private';
import type { RequestHandler } from './$types';


const transporter = nodemailer.createTransport({
	service: 'gmail',
	auth: {
		user: EMAIL_USER,
		pass: EMAIL_PASS
	}
});

export const POST: RequestHandler = async ({ request }) => {
	try {
		// Estrazione dei dati dal corpo della richiesta
		const { nome, cognome, email, messaggio } = await request.json();

		// Validazione dei campi obbligatori
		if (!nome || !cognome || !email || !messaggio) {
			return json({ success: false, message: 'Tutti i campi sono obbligatori' }, { status: 400 });
		}

		// Definizione delle opzioni del messaggio email
		const mailOptions = {
			from: EMAIL_USER, // Utilizza l'indirizzo mittente configurato nel .env
			to: 'norlanstudiobarbano@gmail.com', // Indirizzo di ricezione del portale
			replyTo: email,
			subject: `Nuovo messaggio da ${nome} ${cognome}`,
			text: `
        Nome: ${nome} ${cognome}
        Email: ${email}
        
        Messaggio:
        ${messaggio}
      `,
			html: `
        <h3>Nuovo messaggio dal form di contatto</h3>
        <p><strong>Nome:</strong> ${nome} ${cognome}</p>
        <p><strong>Email:</strong> ${email}</p>
        <p><strong>Messaggio:</strong></p>
        <p>${messaggio.replace(/\n/g, '<br>')}</p>
      `
		};

		// Esecuzione dell'invio tramite l'oggetto transporter
		await transporter.sendMail(mailOptions);

		// Risposta di successo al client
		return json({ success: true, message: 'Email inviata con successo' });
	} catch (error) {
		console.error('Errore nell\'invio dell\'email:', error);
		return json(
			{ success: false, message: 'Si è verificato un errore durante l\'invio dell\'email' },
			{ status: 500 }
		);
	}
};