import java.io.*;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class HuffmanNode implements Comparable<HuffmanNode> {
    char data;
    int frequency;
    HuffmanNode left, right;

    public HuffmanNode(char data, int frequency) {
        this.data = data;
        this.frequency = frequency;
        this.left = this.right = null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return this.frequency - o.frequency;
    }
}

public class fileZipper {

    private static HashMap<Character, String> huffmanCodes = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Huffman Coding Compression and Decompression");
        System.out.println("------------------------------------------");

        System.out.println("1. Compress");
        System.out.println("2. Decompress");
        System.out.print("Enter your choice (1 or 2): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline character

        if (choice == 1) {
            System.out.println("Compression");
            System.out.print("Enter the path of the input file: ");
            String inputFileName = scanner.nextLine();
            System.out.print("Enter the path for the compressed ZIP file: ");
            String compressedZipFileName = scanner.nextLine();

            compress(inputFileName, compressedZipFileName);
            System.out.println("Compression completed successfully.");
        } else if (choice == 2) {
            System.out.println("Decompression");
            System.out.print("Enter the path of the compressed ZIP file: ");
            String compressedZipFileName = scanner.nextLine();
            System.out.print("Enter the path for the decompressed file: ");
            String decompressedFileName = scanner.nextLine();

            decompress(compressedZipFileName, decompressedFileName);
            System.out.println("Decompression completed successfully.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private static void compress(String inputFile, String compressedZipFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             FileOutputStream fos = new FileOutputStream(compressedZipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            StringBuilder stringBuilder = new StringBuilder();
            int ch;

            while ((ch = br.read()) != -1) {
                stringBuilder.append((char) ch);
            }

            char[] charArray = stringBuilder.toString().toCharArray();

            // Build Huffman Tree and generate Huffman Codes
            HuffmanNode root = buildHuffmanTree(charArray);
            generateCodes(root, "", huffmanCodes);

            // Encode the input text using Huffman Codes
            StringBuilder encodedText = new StringBuilder();
            for (char c : charArray) {
                encodedText.append(huffmanCodes.get(c));
            }

            // Write the Huffman Codes and encoded text to a temporary file
            File tempFile = File.createTempFile("tempfile", ".txt");
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(tempFile))) {
                outputStream.writeObject(huffmanCodes);
                outputStream.writeObject(encodedText.toString());
            }

            // Add the temporary file to the ZIP archive
            addToZipFile(tempFile, zos);

            tempFile.delete(); // Delete the temporary file after adding it to the ZIP archive

            System.out.println("Compression and ZIP archiving complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decompress(String compressedZipFile, String decompressedFileName) {
        try (FileInputStream fis = new FileInputStream(compressedZipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                try (ObjectInputStream inputStream = new ObjectInputStream(zis)) {
                    huffmanCodes = (HashMap<Character, String>) inputStream.readObject();
                    String encodedText = (String) inputStream.readObject();

                    // Decode the encoded text using Huffman Codes
                    StringBuilder decodedText = new StringBuilder();
                    int index = 0;
                    while (index < encodedText.length()) {
                        for (char key : huffmanCodes.keySet()) {
                            String code = huffmanCodes.get(key);
                            if (encodedText.startsWith(code, index)) {
                                decodedText.append(key);
                                index += code.length();
                                break;
                            }
                        }
                    }

                    // Write the decoded text to the decompressed file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFileName))) {
                        writer.write(decodedText.toString());
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static HuffmanNode buildHuffmanTree(char[] charArray) {
        HashMap<Character, Integer> frequencyMap = new HashMap<>();
        for (char c : charArray) {
            frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
        }

        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>();
        for (char key : frequencyMap.keySet()) {
            priorityQueue.add(new HuffmanNode(key, frequencyMap.get(key)));
        }

        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.poll();
            HuffmanNode right = priorityQueue.poll();

            HuffmanNode internalNode = new HuffmanNode('\0', left.frequency + right.frequency);
            internalNode.left = left;
            internalNode.right = right;

            priorityQueue.add(internalNode);
        }

        return priorityQueue.poll();
    }

    private static void generateCodes(HuffmanNode root, String code, HashMap<Character, String> huffmanCodes) {
        if (root == null) {
            return;
        }

        if (root.left == null && root.right == null) {
            huffmanCodes.put(root.data, code);
        }

        generateCodes(root.left, code + "0", huffmanCodes);
        generateCodes(root.right, code + "1", huffmanCodes);
    }

    private static void addToZipFile(File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) >= 0) {
            zos.write(buffer, 0, length);
        }

        fis.close();
        zos.closeEntry();
    }
}
