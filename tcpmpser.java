import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class tcpmpser {

    public Integer porto;
    public static Selector selector = null;
    public final static String[] error = { "Non se puido realizar a operacion. ", "Dominio incorrecto.",
            "Resultado fora de rango." };
    public static Map<SocketChannel, Long> acumuladoresTCP = new HashMap<>();
    public static Map<InetSocketAddress, Long> acumuladoresUDP = new HashMap<>();
    public static Map<SocketChannel, Integer> erroNoCalculo = new HashMap<>();
    public static Map<InetSocketAddress, Integer> erroNoCalculoUDP = new HashMap<>();

    public static Long acumuladorUDP = 0L;
    public static Integer erroUDP = 0;

    public static void main(String[] args) {

        comprobaArgs(args);

        int porto = Integer.parseInt(args[0]);

        try {
            selector = Selector.open();

            ServerSocketChannel TCPsocket = ServerSocketChannel.open();
            ServerSocket TCPserverSocket = TCPsocket.socket();
            TCPserverSocket.bind(new InetSocketAddress(porto));
            TCPsocket.configureBlocking(false);
            int ops = TCPsocket.validOps();
            TCPsocket.register(selector, ops, null);

            DatagramChannel UDPsocket = DatagramChannel.open();
            UDPsocket.configureBlocking(false);
            DatagramSocket UDPserverSocket = UDPsocket.socket();
            UDPserverSocket.bind(new InetSocketAddress(porto));
            ops = UDPsocket.validOps();
            UDPsocket.register(selector, ops);

            while (true) {

                selector.select();
                Set<SelectionKey> claves = selector.selectedKeys();
                Iterator<SelectionKey> iterador = claves.iterator();

                while (iterador.hasNext()) {
                    SelectionKey clave = iterador.next();

                    iterador.remove();
                    if (clave.isAcceptable()) {
                        acceptTCP(clave, TCPsocket);
                    } else if (clave.isReadable()) {
                        if (clave.channel() instanceof SocketChannel)
                            readTCP(clave);
                        else if ((clave.channel() instanceof DatagramChannel))
                            readUDP(clave);
                    } else if (clave.isWritable()) {
                        if (clave.channel() instanceof SocketChannel)
                            writeTCP(clave);
                        
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void acceptTCP(SelectionKey clave, ServerSocketChannel socket) throws IOException {
        SocketChannel cliente = socket.accept();
        cliente.configureBlocking(false);
        cliente.register(selector, SelectionKey.OP_READ);
        acumuladoresTCP.put(cliente, 0L);
        erroNoCalculo.put(cliente, 0);
    }

    private static void readTCP(SelectionKey clave) throws IOException {
        SocketChannel socketCliente = (SocketChannel) clave.channel();
        ByteBuffer buffer = ByteBuffer.allocate(64);
        Integer bytesLeidos = socketCliente.read(buffer);
        if (bytesLeidos == -1) {
            clave.cancel();
            socketCliente.close();
        } else {
            buffer.flip();
            byte[] mensaxe = new byte[buffer.limit()];
            buffer.get(mensaxe);
            Integer resultado = calculadoraAcumulador(mensaxe, socketCliente);
            erroNoCalculo.put(socketCliente, resultado);
            socketCliente.register(selector, SelectionKey.OP_WRITE);
        }
    }

    private static void writeTCP(SelectionKey clave) throws IOException {
        SocketChannel socketCliente = (SocketChannel) clave.channel();
        if (erroNoCalculo.get(socketCliente) == 0) {
            // tipo 10 + 16
            byte[] tipo16 = tipo16(acumuladoresTCP.get(socketCliente));
            byte[] tipo10 = tipo10(tipo16);
            ByteBuffer resposta = ByteBuffer.wrap(tipo10);
            socketCliente.write(resposta);
        } else {
            // tipo 10 + 11 + 16
            byte[] tipo11 = tipo11(error[erroNoCalculo.get(socketCliente)]);
            byte[] tipo16 = tipo16(acumuladoresTCP.get(socketCliente));
            byte[] tipo10 = tipo10(tipo11, tipo16);

            ByteBuffer resposta = ByteBuffer.wrap(tipo10);
            socketCliente.write(resposta);
        }
        socketCliente.register(selector, SelectionKey.OP_READ);
    }

    private static void readUDP(SelectionKey clave) throws IOException {
        DatagramChannel socketCliente = (DatagramChannel) clave.channel();

        ByteBuffer buffer = ByteBuffer.allocate(64);
        InetSocketAddress idCliente = (InetSocketAddress) socketCliente.receive(buffer);
        buffer.flip();
        byte[] mensaxe = new byte[buffer.limit()];
        buffer.get(mensaxe);
        erroUDP = calculadoraAcumulador(mensaxe, idCliente.toString());
        socketCliente.register(selector, SelectionKey.OP_WRITE);
        writeUDP(clave, idCliente);
    }

    private static void writeUDP(SelectionKey clave, InetSocketAddress idCliente) throws IOException {
        DatagramChannel socketCliente = (DatagramChannel) clave.channel();
        if (erroUDP == 0) {
            // tipo 10 + 16
            byte[] tipo16 = tipo16(acumuladorUDP);
            byte[] tipo10 = tipo10(tipo16);
            ByteBuffer resposta = ByteBuffer.wrap(tipo10);
            socketCliente.send(resposta, idCliente);
        } else {
            // tipo 10 + 11 + 16
            byte[] tipo11 = tipo11(error[erroUDP]);
            byte[] tipo16 = tipo16(acumuladorUDP);
            byte[] tipo10 = tipo10(tipo11, tipo16);

            ByteBuffer resposta = ByteBuffer.wrap(tipo10);
            socketCliente.send(resposta, idCliente);
        }
        socketCliente.register(selector, SelectionKey.OP_READ);

    }

    public static void comprobaArgs(String[] args) {
        if (args.length != 1) {
            System.err.println("Erro nos argumentos pasados o servidor.\nA sintaxe valida e a seguinte:");
            System.err.println((char) 27 + "[31m" + "\tjava tcpmser porto" + "\u001B[0m");
            System.exit(-1);
        }

    }

    public static int calculadoraAcumulador(byte[] mensaxe, SocketChannel socketCliente) {

        String idCliente;
        long acumulador = acumuladoresTCP.get(socketCliente);
        try {
            idCliente = socketCliente.getRemoteAddress().toString();
            switch (mensaxe[0]) {
                case 1:
                    long suma = mensaxe[2] + mensaxe[3];
                    System.out.println(idCliente + ": " + mensaxe[2] + " + " + mensaxe[3] + " = " + suma);
                    acumuladoresTCP.put(socketCliente, acumulador + suma);

                    return 0;
                case 2:
                    long resta = mensaxe[2] - mensaxe[3];

                    System.out.println(idCliente + ": " + mensaxe[2] + " - " + mensaxe[3] + " = " + resta);
                    acumuladoresTCP.put(socketCliente, acumulador + resta);

                    return 0;
                case 3:

                    long producto = mensaxe[2] * mensaxe[3];

                    System.out.println(idCliente + ": " + mensaxe[2] + " * " + mensaxe[3] + " = " + producto);
                    acumuladoresTCP.put(socketCliente, acumulador + producto);

                    return 0;
                case 4:

                    if (mensaxe[3] == 0) {
                        System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1] + "\u001B[0m");
                        return 1;
                    }

                    long cociente = mensaxe[2] / mensaxe[3];
                    System.out.println(idCliente + ": " + mensaxe[2] + " / " + mensaxe[3] + " = " + cociente);
                    acumuladoresTCP.put(socketCliente, acumulador + cociente);
                    return 0;
                case 5:

                    if (mensaxe[3] == 0) {
                        System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1] + "\u001B[0m");
                        return 1;
                    }

                    long resto = mensaxe[2] % mensaxe[3];
                    System.out.println(idCliente + ": " + mensaxe[2] + " % " + mensaxe[3] + " = " + resto);
                    acumuladoresTCP.put(socketCliente, acumulador + resto);
                    return 0;
                case 6:
                    long factorial = 0;

                    if (mensaxe[2] > 20) {
                        System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[2] + "\u001B[0m");
                        return 2;
                    }

                    factorial = factorial(mensaxe[2]);

                    if (factorial == -1) {
                        System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1]  + "\u001B[0m");
                        return 1;
                    }

                    System.out.println(idCliente + ": " + mensaxe[2] + "!" + " = " + factorial);
                    acumuladoresTCP.put(socketCliente, acumulador + factorial);
                    return 0;
            }
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            return 2;
        }
    }

    public static int calculadoraAcumulador(byte[] mensaxe, String idCliente) throws IOException {
        switch (mensaxe[0]) {
            case 1:
                long suma = mensaxe[2] + mensaxe[3];
                System.out.println(idCliente + ": " + mensaxe[2] + " + " + mensaxe[3] + " = " + suma);
                acumuladorUDP = acumuladorUDP + suma;

                return 0;
            case 2:
                long resta = mensaxe[2] - mensaxe[3];

                System.out.println(idCliente + ": " + mensaxe[2] + " - " + mensaxe[3] + " = " + resta);
                acumuladorUDP = acumuladorUDP + resta;

                return 0;
            case 3:

                long producto = mensaxe[2] * mensaxe[3];

                System.out.println(idCliente + ": " + mensaxe[2] + " * " + mensaxe[3] + " = " + producto);
                acumuladorUDP = acumuladorUDP + producto;

                return 0;
            case 4:

                if (mensaxe[3] == 0) {
                    System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1] + "\u001B[0m");
                    return 1;
                }

                long cociente = mensaxe[2] / mensaxe[3];
                System.out.println(idCliente + ": " + mensaxe[2] + " / " + mensaxe[3] + " = " + cociente);
                acumuladorUDP = acumuladorUDP + cociente;
                return 0;
            case 5:

                if (mensaxe[3] == 0) {
                    System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1] + "\u001B[0m");
                    return 1;
                }

                long resto = mensaxe[2] % mensaxe[3];
                System.out.println(idCliente + ": " + mensaxe[2] + " % " + mensaxe[3] + " = " + resto);
                acumuladorUDP = acumuladorUDP + resto;
                return 0;
            case 6:
                long factorial = 0;

                if (mensaxe[2] > 20) {
                    System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[2] + "\u001B[0m");
                    return 2;
                }

                factorial = factorial(mensaxe[2]);

                if (factorial == -1) {
                    System.err.println(idCliente + ": " + (char) 27 + "[31m" + error[0] + error[1] + "\u001B[0m");
                    return 1;
                }

                System.out.println(idCliente + ": " + mensaxe[2] + "!" + " = " + factorial);
                acumuladorUDP = acumuladorUDP + factorial;
                return 0;
        }
        return 1;
    }

    public static long factorial(int N1) {

        if (N1 < 0)
            return -1;

        long factorial = 1;

        for (long i = 1; i <= N1; i++) {
            factorial *= i;
        }
        return factorial;
    }

    public static byte[] tipo10(byte[] tipo11, byte[] tipo16) {

        byte[] bytes = { 10, (byte) (tipo11.length + tipo16.length) };

        byte[] data = new byte[tipo11.length + tipo16.length];
        System.arraycopy(tipo11, 0, data, 0, tipo11.length);
        System.arraycopy(tipo16, 0, data, tipo11.length, tipo16.length);

        byte[] result = new byte[bytes.length + data.length];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        System.arraycopy(data, 0, result, bytes.length, data.length);

        return result;
    }

    public static byte[] tipo10(byte[] tipo16) {

        byte[] bytes = { 10, (byte) (tipo16.length) };

        byte[] result = new byte[bytes.length + tipo16.length];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        System.arraycopy(tipo16, 0, result, bytes.length, tipo16.length);

        return result;
    }

    public static byte[] tipo11(String error) {

        byte[] bytes = { 11, (byte) (error.length()) };

        try {
            byte[] strBytes = error.getBytes("UTF-8");

            byte[] result = Arrays.copyOf(bytes, bytes.length + strBytes.length);

            System.arraycopy(strBytes, 0, result, bytes.length, strBytes.length);

            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static byte[] tipo16(long acumulador) {

        byte result[] = new byte[] {
                16,
                8,
                (byte) ((acumulador >> 56) & 0xff),
                (byte) ((acumulador >> 48) & 0xff),
                (byte) ((acumulador >> 40) & 0xff),
                (byte) ((acumulador >> 32) & 0xff),
                (byte) ((acumulador >> 24) & 0xff),
                (byte) ((acumulador >> 16) & 0xff),
                (byte) ((acumulador >> 8) & 0xff),
                (byte) ((acumulador >> 0) & 0xff),
        };
        return result;
    }

    public static void printByteArrayAsInts(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print((int) array[i] + " ");
        }
        System.out.println();
    }
}
