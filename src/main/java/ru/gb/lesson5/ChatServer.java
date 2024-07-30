package ru.gb.lesson5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

  private final static ObjectMapper objectMapper = new ObjectMapper();

  // Socket - абстракция, к которой можно подключиться
  // ip-address + port - socket
  // network - сеть - набор соединенных устройств
  // ip-address - это адрес устройства в какой-то сети
  // 8080 - http
  // 443 - https
  // 35 - smtp
  // 21 - ftp
  // 5432 - стандартный порт postgres
  // клиент подключается к серверу

  /**
   * Порядок взаимодействия:
   * 1. Клиент подключается к серверу
   * 2. Клиент посылает сообщение, в котором указан логин. Если на сервере уже есть подключеный клиент с таким логином, то соедение разрывается
   * 3. Клиент может посылать 3 типа команд:
   * 3.1 list - получить логины других клиентов
   * <p>
   * 3.2 send @login message - отправить личное сообщение с содержимым message другому клиенту с логином login
   * 3.3 send message - отправить сообщение всем с содержимым message
   */

  // 1324.132.12.3:8888
  public static void main(String[] args) {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    try (ServerSocket server = new ServerSocket(8888)) {
      System.out.println("Сервер запущен");

      while (true) {
        System.out.println("Ждем клиентского подключения");
        Socket client = server.accept();
        ClientHandler clientHandler = new ClientHandler(client, clients);
        new Thread(clientHandler).start();
      }
    } catch (IOException e) {
      System.err.println("Ошибка во время работы сервера: " + e.getMessage());
    }
  }

  private static class ClientHandler implements Runnable {

    private final Socket client;
    private final Scanner in;
    private final PrintWriter out;
    private final Map<String, ClientHandler> clients;
    private String clientLogin;

    public ClientHandler(Socket client, Map<String, ClientHandler> clients) throws IOException {
      this.client = client;
      this.clients = clients;

      this.in = new Scanner(client.getInputStream());
      this.out = new PrintWriter(client.getOutputStream(), true);
    }

    @Override
    public void run() {
      System.out.println("Подключается новый клиент");
      while (true) {
        try {
          String loginRequest = in.nextLine();
          LoginRequest request = objectMapper.reader().readValue(loginRequest, LoginRequest.class);
          this.clientLogin = request.getLogin();
        } catch (IOException e) {
          System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
          String unsuccessfulResponse = createLoginResponse(false);
          out.println(unsuccessfulResponse);
          doClose();
          return;
        }

        System.out.println("Запрос от клиента: " + clientLogin);
        // Выход из цикла, есле логин не занят
        if (!clients.containsKey(clientLogin)) {
          System.out.println("Пользователь " + clientLogin + ": успешный вход в систему");
          break;
        }

        // Если логин занят, посылаем сообщение об ошибке подключения, новый цикл
        String unsuccessfulResponse = createLoginResponse(false);
        out.println(unsuccessfulResponse);
        System.err.println("Пользователь с логином " + clientLogin + " уже существует");
      }

      clients.put(clientLogin, this);
      String successfulLoginResponse = createLoginResponse(true);
      out.println(successfulLoginResponse);
      while (true) {
        String msgFromClient = in.nextLine();
        final String type;
        try {
          AbstractRequest request = objectMapper.reader().readValue(msgFromClient, AbstractRequest.class);
          type = request.getType();
        } catch (IOException e) {
          System.err.println("Не удалось прочитать сообщение от пользователя " + clientLogin + ": " + e.getMessage());
          sendMessage("Не удалось прочитать сообщение: " + e.getMessage());
          continue;
        }

        if (SendMessageRequest.TYPE.equals(type)) {
          try {
            sendMessageFromClient(msgFromClient);
          } catch (IOException e) {
            sendMessage("Не удалось прочитать сообщение sendMessageRequest: " + e.getMessage());
          }
        } else if (BroadcastRequest.TYPE.equals(type)) {
          try {
            sendBroadcastMessage(msgFromClient);
          } catch (IOException e) {
            sendMessage("Не удалось прочитать сообщение BroadcastMessageRequest: " + e.getMessage());
          }
        } else if (DisconnectRequest.TYPE.equals(type)) { // DisconnectRequest.TYPE.equals(type)
          System.out.println("Отключаем " + this.clientLogin);
          DisconnectClient();
          return;
        } else if (ListClientsRequest.TYPE.equals(type)) { // DisconnectRequest.TYPE.equals(type)
          try {
            getListClient();
          } catch (IOException e) {
            sendMessage("Не удалось получить список пользователей: " + e.getMessage());
          }
        } else {
          System.err.println("Неизвестный тип сообщения: " + type);
          sendMessage("Неизвестный тип сообщения: " + type);
        }
      }

    }

    private boolean sendMessageFromClient(String msgFromClient) throws IOException {
      SendMessageRequest request;
      request = objectMapper.reader().readValue(msgFromClient, SendMessageRequest.class);


      ClientHandler clientTo = clients.get(request.getRecipient());
      if (clientTo == null) {
        sendMessage("Пользователь " + request.getRecipient() + " отсутствует");
        return false;
      }
      clientTo.sendMessage(this.clientLogin + ": " + request.getMessage());
      return true;
    }

    private void DisconnectClient() {
      clients.remove(clientLogin);
      String message = "Пользователь " + clientLogin + " вышел из чата";
      clients.keySet().forEach(c -> clients.get(c).sendMessage(message));
      System.out.println(message);
      doClose();
    }

    private void doClose() {
      try {
        in.close();
        out.close();
        client.close();
      } catch (IOException e) {
        System.err.println("Ошибка во время отключения клиента: " + e.getMessage());
      }
    }

    public void sendMessage(String message) {
      // TODO: нужно придумать структуру сообщения
      out.println(message);
    }



    private void sendBroadcastMessage(String messageFromClient) throws IOException {
      BroadcastRequest request;
      try {
        request = objectMapper.reader().readValue(messageFromClient, BroadcastRequest.class);
        String login = this.clientLogin;
        ClientHandler client = clients.get(login);
        if (client == null) {
          sendMessage("Сервер сообщает: Пользователь " + login + " отсутствует");
        } else {
          String msg = request.getMessage();
          clients.keySet().stream()
                  .filter(c -> !c.equals(login))
                  .forEach(c -> clients.get(c).sendMessage(this.clientLogin + ": " + msg));

        }
      } catch (IOException e) {
        System.err.println("Не удалось прочитать сообщение от " + clientLogin + ": " + e.getMessage());
        throw new IOException(e.getMessage());
      }
    }

    private void getListClient() throws IOException {
      ListResponse response = new ListResponse();
      try {
        List<User> users = new ArrayList<>();
        for (String client : clients.keySet()) {
          if (!client.equals(clientLogin)) {
            User user = new User();
            user.setLogin(client);
            users.add(user);
          }
        }
        response.setUsers(users);
        String sendMsgRequest = objectMapper.writeValueAsString(response);
        sendMessage(sendMsgRequest);
      } catch (IOException e) {
        System.err.println("Не удалось прочитать сообщение от клиента [" + clientLogin + "]: " + e.getMessage());
        throw new IOException(e.getMessage());
      }
    }

    private String createLoginResponse(boolean success) {
      LoginResponse loginResponse = new LoginResponse();
      loginResponse.setConnected(success);
      try {
        return objectMapper.writer().writeValueAsString(loginResponse);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Не удалось создать loginResponse: " + e.getMessage());
      }
    }

  }

}
