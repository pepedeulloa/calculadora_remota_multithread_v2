import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class tcpmpcli {

    public static InetSocketAddress ipPortoServer;
    public static Boolean UDPcli = false;
    public static Integer N1, N2, tipo, lonx;

    public ByteBuffer buffer;

    public static String[] msg = { "" };
    public static String charset = "latin1";

    public static String msgError = (char) 27 + "[31m" + "Non se puido interpretar a operacion. Proba outra vez"
            + "\u001B[0m";

    public static void main(String[] args) {
        comprobaArgs(args);
        if (UDPcli) {
            ipPortoServer = new InetSocketAddress(args[1], Integer.parseInt(args[2]));
            UDPcli();
        } else {
            ipPortoServer = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
            TCPcli();
        }

        return;
    }

    private static void TCPcli() {
        try {
            SocketChannel socket = SocketChannel.open();
            socket.connect(ipPortoServer);

            int flag = 0;

            System.out.println("\nEscriba a operacion a enviar o servidor TCP.");
            System.out.println("Separe mediante espacios en branco os operandos e a operacion.");
            System.out.println("As operacions soportadas son: +, -, /, *, % e !.\nPor exemplo: 5 ! ou -5 + 5.");
            System.out.println("Envie 'QUIT' para Sair");

            while (true) {

                flag = leeTeclado();

                if (flag == -1) {
                    System.out.println(msgError);
                } else {

                    if (flag == 1) {
                        tipo = 0;
                    } else
                        tipo = comprobaTipo();
                    if ((msg.length == 2 && tipo != 6) || tipo == -1) {
                        System.err.println(msgError);
                    } else if ((msg.length == 3 && tipo == 6)) {
                        System.err.println(msgError);
                    } else {

                        if (tipo == 6) {
                            N1 = Integer.parseInt(msg[0]);
                            lonx = 1;
                            N2 = 0;
                        } else if (tipo == 0) {
                            N1 = 0;
                            N2 = 0;
                            lonx = 0;
                            break;
                        } else {
                            N1 = Integer.parseInt(msg[0]);
                            lonx = 2;
                            N2 = Integer.parseInt(msg[2]);
                        }

                        if (comprobaNums(N1, N2) == -1) {
                            System.err.println(msgError);
                        } else {
                            byte[] envio = numerosTLV();
                            write(socket, envio);

                            read(socket);

                        }
                    }
                }

            }

            socket.close();

        } catch (IOException e) {
            System.err.println(
                    (char) 27 + "[31m" + "Conexion rexeitada, revise a direccion e o porto do servidor." + "\u001B[0m");
        }
    }

    private static void UDPcli() {
        try {
            DatagramChannel socket = DatagramChannel.open();
            int flag = 0;

            System.out.println("\nEscriba a operacion a enviar o servidor UDP.");
            System.out.println("Separe mediante espacios en branco os operandos e a operacion.");
            System.out.println("As operacions soportadas son: +, -, /, *, % e !.\nPor exemplo: 5 ! ou -5 + 5.");
            System.out.println("Envie 'QUIT' para Sair");

            while (true) {

                flag = leeTeclado();

                if (flag == -1) {
                    System.out.println(msgError);
                } else {

                    if (flag == 1) {
                        tipo = 0;
                    } else
                        tipo = comprobaTipo();
                    if ((msg.length == 2 && tipo != 6) || tipo == -1) {
                        System.err.println(msgError);
                    } else if ((msg.length == 3 && tipo == 6)) {
                        System.err.println(msgError);
                    } else {

                        if (tipo == 6) {
                            N1 = Integer.parseInt(msg[0]);
                            lonx = 1;
                            N2 = 0;
                        } else if (tipo == 0) {
                            N1 = 0;
                            N2 = 0;
                            lonx = 0;
                            break;
                        } else {
                            N1 = Integer.parseInt(msg[0]);
                            lonx = 2;
                            N2 = Integer.parseInt(msg[2]);
                        }

                        if (comprobaNums(N1, N2) == -1) {
                            System.err.println(msgError);
                        } else {
                            byte[] envio = numerosTLV();
                            write(socket, envio);

                            read(socket);

                        }
                    }
                }

            }

            socket.close();

        } catch (IOException e) {
            System.err.println(
                    (char) 27 + "[31m" + "Conexion rexeitada, revise a direccion e o porto do servidor." + "\u001B[0m");
        }
    }

    private static void write(SocketChannel socket, byte[] envio) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(envio);
        socket.write(buffer);
    }

    private static void write(DatagramChannel socket, byte[] envio) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(envio);
        socket.send(buffer, ipPortoServer);
    }

    private static void read(SocketChannel socket) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(64);
        socket.read(buffer);
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        int[] cabeceira = { data[0], data[1], data[2], data[3] };
        byte[] datos = Arrays.copyOfRange(data, 4, data.length);

        if (cabeceira[2] == 16) {
            long longRecibido = byteToLongAcumulador(datos);
            System.out.println("Valor do acumulador: " + longRecibido);
        } else {
            String error = byteToStringError(datos, cabeceira[3]);
            long longRecibido = byteToLongAcumulador(datos, cabeceira[3]);

            System.out.println("Valor do acumulador: " + longRecibido + ". " + (char) 27 + "[31m"
                    + "Error: " + error + "\u001B[0m");
        }
    }

    private static void read(DatagramChannel socket) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(64);
        socket.receive(buffer);
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        int[] cabeceira = { data[0], data[1], data[2], data[3] };
        byte[] datos = Arrays.copyOfRange(data, 4, data.length);

        if (cabeceira[2] == 16) {
            long longRecibido = byteToLongAcumulador(datos);
            System.out.println("Valor do acumulador: " + longRecibido);
        } else {
            String error = byteToStringError(datos, cabeceira[3]);
            long longRecibido = byteToLongAcumulador(datos, cabeceira[3]);

            System.out.println("Valor do acumulador: " + longRecibido + ". " + (char) 27 + "[31m"
                    + "Error: " + error + "\u001B[0m");
        }
    }

    private static long byteToLongAcumulador(byte[] datos, int lonxitude) {
        byte[] acumulador = Arrays.copyOfRange(datos, lonxitude + 2, datos.length);
        long longRecibido = ((acumulador[0] & 0xFFL) << 56) |
                ((acumulador[1] & 0xFFL) << 48) |
                ((acumulador[2] & 0xFFL) << 40) |
                ((acumulador[3] & 0xFFL) << 32) |
                ((acumulador[4] & 0xFFL) << 24) |
                ((acumulador[5] & 0xFFL) << 16) |
                ((acumulador[6] & 0xFFL) << 8) |
                ((acumulador[7] & 0xFFL) << 0);
        return longRecibido;

    }

    private static long byteToLongAcumulador(byte[] acumulador) {

        long longRecibido = ((acumulador[0] & 0xFFL) << 56) |
                ((acumulador[1] & 0xFFL) << 48) |
                ((acumulador[2] & 0xFFL) << 40) |
                ((acumulador[3] & 0xFFL) << 32) |
                ((acumulador[4] & 0xFFL) << 24) |
                ((acumulador[5] & 0xFFL) << 16) |
                ((acumulador[6] & 0xFFL) << 8) |
                ((acumulador[7] & 0xFFL) << 0);
        return longRecibido;

    }

    private static String byteToStringError(byte[] datos, int lonxitude) throws UnsupportedEncodingException {
        byte[] stringBytes = Arrays.copyOfRange(datos, 0, lonxitude);
        return new String(stringBytes, "UTF-8");
    }

    public static int leeTeclado() {

        String mensaxe = "";
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        try {

            mensaxe = teclado.readLine();

            msg = mensaxe.split("\\s");

            if (mensaxe.equals("QUIT")) {
                return 1;
            }

            if (msg.length < 2 || msg.length > 3) {
                return -1;
            } else {
                msg[1] = msg[1].trim();
            }

        } catch (IOException e) {

            System.err.println((char) 27 + "[31m" + "Erro na lectura." + "\u001B[0m");
            System.exit(-1);

        }

        return 0;
    }

    public static int comprobaTipo() {

        switch (msg[1]) {
            case "+":
                return 1;
            case "-":
                return 2;
            case "*":
                return 3;
            case "/":
                return 4;
            case "%":
                return 5;
            case "!":
                return 6;
            default:
                return -1;
        }
    }

    public static int comprobaNums(int N1, int N2) {
        final int LIMITE_MAX = 127;
        final int LIMITE_MIN = -127;

        if (N1 > LIMITE_MAX || N1 < LIMITE_MIN || N2 > LIMITE_MAX || N2 < LIMITE_MIN)
            return -1;
        else
            return 0;
    }

    public static void comprobaArgs(String[] args) {

        if (args.length < 2 || args.length > 3) {
            System.err.println("Erro nos argumentos pasados o cliente.\nA sintaxe valida e a seguinte:");
            System.err.println((char) 27 + "[31m" + "\tTCP: tcpmpcli ip_servidor porto" + "\u001B[0m");
            System.err.println((char) 27 + "[31m" + "\tUDP: tcpmpcli -u ip_servidor porto" + "\u001B[0m");
            System.exit(-1);
        }
        
        for (int i = 0; i< args.length; i++){
            if(args[i].equals("-u"))
                UDPcli = true;
        }
    }

    public static byte[] numerosTLV() {

        if (tipo == 6) {
            byte bytes[] = { tipo.byteValue(), lonx.byteValue(), N1.byteValue() };
            return bytes;
        } else {
            byte bytes[] = { tipo.byteValue(), lonx.byteValue(), N1.byteValue(), N2.byteValue() };
            return bytes;
        }

    }

    public static void printByteArrayAsInts(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print((int) array[i] + " ");
        }
        System.out.println();
    }

}
