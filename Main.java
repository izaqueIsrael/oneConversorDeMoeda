import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Configuração do Ambiente Java ===");
        System.out.println("Versão mínima recomendada: Java 8 ou superior.\n");

        System.out.println("=== Consumo da API ===");
        String apiKey = "";
        String baseCurrency = "USD";

        String urlString = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + baseCurrency;

        String jsonResponse = getJsonResponse(urlString);
        if (jsonResponse.isEmpty()) {
            System.out.println("Não foi possível obter resposta da API. Verifique a URL ou sua chave de API.");
            return;
        }

        Map<String, Double> taxasDeCambio = extrairTaxasConversao(jsonResponse);

        if (taxasDeCambio.isEmpty()) {
            System.out.println("Não foi possível extrair as taxas de câmbio do JSON.");
            return;
        } else {
            System.out.println("Taxas de câmbio encontradas!");
            System.out.println("Moeda Base: " + baseCurrency);
        }

        System.out.println("\n=== Filtro de Moedas ===");
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Digite o código de moeda que deseja filtrar (ex: BRL, EUR, GBP): ");
            String moedaFiltro = scanner.nextLine().toUpperCase().trim();

            if (taxasDeCambio.containsKey(moedaFiltro)) {
                double taxa = taxasDeCambio.get(moedaFiltro);
                System.out.printf("Taxa de conversão de %s para %s: %.4f%n", baseCurrency, moedaFiltro, taxa);
            } else {
                System.out.printf("A moeda '%s' não foi encontrada na lista de taxas de câmbio.%n", moedaFiltro);
            }
        }

        System.out.println("\n=== Exibição de Resultados ===");
        System.out.println("Lista completa de taxas de câmbio:");
        taxasDeCambio.keySet().stream().sorted().forEach(moeda -> {
            double taxa = taxasDeCambio.get(moeda);
            System.out.printf("%s: %.4f%n", moeda, taxa);
        });

        System.out.println("\nOperação concluída.");
    }

    private static String getJsonResponse(String urlString) {
        StringBuilder resposta = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
            conexao.setRequestMethod("GET");

            int statusResposta = conexao.getResponseCode();
            if (statusResposta == 200) {
                try (BufferedReader leitor = new BufferedReader(new InputStreamReader(conexao.getInputStream()))) {
                    String linha;
                    while ((linha = leitor.readLine()) != null) {
                        resposta.append(linha);
                    }
                }
            } else {
                System.out.println("Erro na requisição. Código de status: " + statusResposta);
            }
        } catch (Exception e) {
            System.out.println("Erro ao obter resposta da API: " + e.getMessage());
        }
        return resposta.toString();
    }

    private static Map<String, Double> extrairTaxasConversao(String json) {
        Map<String, Double> mapa = new HashMap<>();

        try {
            int indiceConversionRates = json.indexOf("\"conversion_rates\"");
            if (indiceConversionRates == -1) {
                return mapa;
            }

            int inicioObjeto = json.indexOf("{", indiceConversionRates);
            if (inicioObjeto == -1) {
                return mapa;
            }

            int contadorChaves = 0;
            int fimObjeto = -1;

            for (int i = inicioObjeto; i < json.length(); i++) {
                if (json.charAt(i) == '{') {
                    contadorChaves++;
                } else if (json.charAt(i) == '}') {
                    contadorChaves--;
                    if (contadorChaves == 0) {
                        fimObjeto = i;
                        break;
                    }
                }
            }

            if (fimObjeto == -1) {
                return mapa;
            }

            String jsonConversionRates = json.substring(inicioObjeto + 1, fimObjeto).trim();
            String[] pares = jsonConversionRates.split(",");

            for (String par : pares) {
                par = par.trim();
                if (par.isEmpty()) {
                    continue;
                }

                int posicaoDoisPontos = par.indexOf(":");
                if (posicaoDoisPontos == -1) {
                    continue;
                }

                String moeda = par.substring(0, posicaoDoisPontos).replace("\"", "").trim();
                String taxaStr = par.substring(posicaoDoisPontos + 1).replace("\"", "").trim();

                try {
                    double taxa = Double.parseDouble(taxaStr);
                    mapa.put(moeda, taxa);
                } catch (NumberFormatException | NullPointerException ignored) {
                    // Tratamento consolidado para exceções específicas
                }
            }

        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            System.out.println("Erro no formato dos dados JSON: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro genérico ao analisar o JSON: " + e.getMessage());
        }

        return mapa;
    }
}
