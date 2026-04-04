package com.vicente.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagerApplication {
    static void main(String[] args) {
		SpringApplication.run(TaskManagerApplication.class, args);
	}
}

/*
 *		Abre a URL assinada diretamente no navegador padrão do sistema (macOS)
 * 		Não funciona em ambientes headless (Docker, servidor remoto, CI, etc.).
 * 		Executa um comando do sistema operacional para abrir a URL no navegador padrão.
 * 		"open" é um comando do macOS que abre arquivos ou URLs.
 * 		Runtime.getRuntime().exec(...) executa processos externos ao Java.
 * 		 Isso é útil apenas para debug local, evitando copiar manualmente a URL grande.
 * 		Não deve ser usado em produção nem em ambientes sem interface gráfica.
*/
//		Runtime.getRuntime().exec(new String[]{"open", signedUrl});


