import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * 1) Configura o ambiente Java
 * 2) Cria um projeto simples
 * 3) Consome a API da exchangerate-api.com
 * 4) Analisa a resposta JSON
 * 5) Filtra moedas com base em um código digitado pelo usuário
 * 6) Exibe os resultados
 *
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=== 1) Configuração do Ambiente Java ===");
        System.out.println("Certifique-se de ter o Java instalado e configurado.");
        System.out.println("Versão mínima recomendada: Java 8 ou superior.\n");

        System.out.println("=== 2) Criação do Projeto ===");
        System.out.println("Para compilar e executar: ");
        System.out.println("   javac ProjetoCambio.java");
        System.out.println("   java ProjetoCambio\n");

        System.out.println("=== 3) Consumo da API ===");

        String apiKey = "301cc199b9551fa032006359";
        String baseCurrency = "USD";

        String urlString = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/" + baseCurrency;

        String jsonResponse = getJsonResponse(urlString);
        if (jsonResponse.isEmpty()) {
            System.out.println("Não foi possível obter resposta da API. Verifique a URL ou sua chave de API.");
            return;
        }

        System.out.println("\n=== 4) Análise da Resposta JSON ===");
        Map<String, Double> taxasDeCambio = extrairTaxasConversao(jsonResponse);

        if (taxasDeCambio.isEmpty()) {
            System.out.println("Não foi possível extrair as taxas de câmbio do JSON. Verifique se o formato está correto.");
            return;
        } else {
            System.out.println("Taxas de câmbio encontradas com sucesso!");
            System.out.println("Moeda Base: " + baseCurrency);
        }

        System.out.println("\n=== 5) Filtro de Moedas ===");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o código de moeda que deseja filtrar (ex: BRL, EUR, GBP): ");
        String moedaFiltro = scanner.nextLine().toUpperCase().trim();

        if (taxasDeCambio.containsKey(moedaFiltro)) {
            double taxa = taxasDeCambio.get(moedaFiltro);
            System.out.printf("Taxa de conversão de %s para %s: %.4f%n", baseCurrency, moedaFiltro, taxa);
        } else {
            System.out.printf("A moeda '%s' não foi encontrada na lista de taxas de câmbio.%n", moedaFiltro);
        }

        System.out.println("\n=== 6) Exibição de Resultados ===");
        System.out.println("Lista completa de taxas de câmbio obtidas:");
        taxasDeCambio
            .keySet()
            .stream()
            .sorted()
            .forEach(moeda -> {
                double taxa = taxasDeCambio.get(moeda);
                System.out.printf("%s: %.4f%n", moeda, taxa);
            });

        scanner.close();
        System.out.println("\nDesafio concluído!.\n");
    }

    /**
     * Faz uma requisição GET na URL informada e retorna o conteúdo em formato String (JSON).
     * 
     * @param urlString A URL para onde será enviada a requisição GET.
     * @return O conteúdo da resposta (JSON) em forma de String ou vazio em caso de erro.
     */
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

            conexao.disconnect();
        } catch (Exception e) {
            System.out.println("Erro ao obter resposta da API: " + e.getMessage());
        }
        return resposta.toString();
    }

    /**
     * Extrai o objeto "conversion_rates" do JSON retornado pela API e mapeia cada par
     * "CÓDIGO_MOEDA": taxa para dentro de um Map.
     *
     * Observação: este método faz o parsing do JSON de forma manual e simples, sem uso
     * de bibliotecas especializadas. Em produção, prefira usar Jackson, Gson ou outras.
     * @param json String que representa o conteúdo JSON retornado pela API.
     * @return Um Map contendo pares do tipo { "CÓDIGO_MOEDA" -> taxa }.
     */
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

            String jsonConversionRates = json.substring(inicioObjeto, fimObjeto + 1).trim();
            if (jsonConversionRates.startsWith("{")) {
                jsonConversionRates = jsonConversionRates.substring(1);
            }
            if (jsonConversionRates.endsWith("}")) {
                jsonConversionRates = jsonConversionRates.substring(0, jsonConversionRates.length() - 1);
            }

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

                String moeda = par.substring(0, posicaoDoisPontos).trim();
                moeda = moeda.replace("\"", "");

                String taxaStr = par.substring(posicaoDoisPontos + 1).trim();
                taxaStr = taxaStr.replace("\"", "");

                try {
                    double taxa = Double.parseDouble(taxaStr);
                    mapa.put(moeda, taxa);
                } catch (NumberFormatException e) {
                }
            }

        } catch (Exception e) {
            System.out.println("Erro ao analisar o JSON: " + e.getMessage());
        }

        return mapa;
    }
}
