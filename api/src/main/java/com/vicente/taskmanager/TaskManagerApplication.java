package com.vicente.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskManagerApplication{
//	@Value("${app.storage.azure-blob.connection-string}")
//	private String cString;

//	private final StorageService storageService;
//
//    public TaskManagerApplication(StorageService storageService) {
//        this.storageService = storageService;
//    }

    static void main(String[] args) {
		SpringApplication.run(TaskManagerApplication.class, args);
	}

//	@Override
//	public void run(String... args) throws Exception {
//		String connectionString = cString;
//		String containerName = "taskmanager-files";
//
//		BlobContainerClient containerClient = new BlobContainerClientBuilder()
//				.connectionString(connectionString)
//				.containerName(containerName)
//				.buildClient();
//
//		System.out.println("Blobs in container:");
//		for (BlobItem blobItem : containerClient.listBlobs()) {
//			System.out.println(" - " + blobItem.getName());
//		}
//
//		String objectKey = "81/tasks/1109/f0afec14-71f1-4969-8b7a-b1d5ab1ced53.pdf";
//		BlobClient blobClient = containerClient.getBlobClient(objectKey);
//
//		if (blobClient.exists()) {
//			blobClient.delete();
//			System.out.println("Blob deletado: " + objectKey);
//		} else {
//			System.out.println("Blob não encontrado: " + objectKey);
//		}
//
//		for (BlobItem blobItem : containerClient.listBlobs()) {
//			System.out.println(" - " + blobItem.getName());
//		}


////		Path filePath = Paths.get("/Users/vicentejluz/Downloads/taskmanager/logs/taskmanager-7.log");
////
////		String mimeType = Files.probeContentType(filePath);
////
////		if (mimeType == null) {
////			mimeType = "text/plain";
////		}
////
////		try (InputStream inputStream = Files.newInputStream(filePath)) {
////			String fileName =  "logs/test2.log";
////
////			fileStorageService.upload(inputStream,  inputStream.available(), fileName, mimeType);
////		}
//
//		String keyTeste = "logs/test.log"; // Use uma chave que você sabe que existe no S3/MinIO
//
//		Path test = Paths.get("test/test.log");
//
//		// Pega o diretório pai (no caso, a pasta "test")
//		Path parentDir = test.getParent();
//
//		// Se houver um diretório pai e ele não existir, cria ele
//		if (parentDir != null && Files.notExists(parentDir)) {
//			Files.createDirectories(parentDir);
//		}
//
//
//		try (InputStream is = storageService.download(keyTeste)) {
//			// Opção 1: Apenas ler os bytes para confirmar que não deu erro
//			Files.copy(is, test, StandardCopyOption.REPLACE_EXISTING);
//
//			System.out.println("Download concluído com sucesso!");
//			System.out.println("Arquivo salvo em: " + test.toAbsolutePath());
//			// Opção 2: Se for um arquivo de texto, você pode imprimir uma parte
//			// String texto = new String(content, StandardCharsets.UTF_8);
//			// System.out.println("Conteúdo: " + texto.substring(0, Math.min(texto.length(), 50)));
//
//		} catch (StorageException e) {
//			System.err.println("Erro no teste de download! Status: " + e.getStatusCode());
//			System.err.println("Mensagem: " + e.getMessage());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}


