package br.gov.sp.cps.combustivel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etData, etHodometro, etLitros, etValorLitro;
    private RadioGroup rgCombustivel;
    private Button btnCalcular, btnLimparDados;

    private TextView tvDistancia, tvConsumoMedio, tvCustoKm, tvKmDia;

    private TextInputEditText etPrecoEtanol, etPrecoGasolina;
    private Button btnComparar;
    private TextView tvResultadoComparativo;

    private static final String PREFS_NAME = "FuelControlPrefs";
    private static final String KEY_LAST_ODO = "last_odometer";
    private static final String KEY_LAST_DATE = "last_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarComponentes();
        preencherDataAtual();

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calcularAbastecimento();
            }
        });

        btnComparar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compararCombustiveis();
            }
        });

        // Evento de clique para o botão de limpar dados
        btnLimparDados.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                limparDadosSalvos();
            }
        });
    }

    private void inicializarComponentes() {
        etData = findViewById(R.id.etData);
        etHodometro = findViewById(R.id.etHodometro);
        etLitros = findViewById(R.id.etLitros);
        etValorLitro = findViewById(R.id.etValorLitro);
        rgCombustivel = findViewById(R.id.rgCombustivel);
        btnCalcular = findViewById(R.id.btnCalcular);
        btnLimparDados = findViewById(R.id.btnLimparDados);

        tvDistancia = findViewById(R.id.tvDistancia);
        tvConsumoMedio = findViewById(R.id.tvConsumoMedio);
        tvCustoKm = findViewById(R.id.tvCustoKm);
        tvKmDia = findViewById(R.id.tvKmDia);

        etPrecoEtanol = findViewById(R.id.etPrecoEtanol);
        etPrecoGasolina = findViewById(R.id.etPrecoGasolina);
        btnComparar = findViewById(R.id.btnComparar);
        tvResultadoComparativo = findViewById(R.id.tvResultadoComparativo);
    }

    private void preencherDataAtual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etData.setText(sdf.format(new Date()));
    }

    private void calcularAbastecimento() {
        String dataStr = etData.getText().toString().trim();
        String odoStr = etHodometro.getText().toString().trim();
        String litrosStr = etLitros.getText().toString().trim();
        String valorStr = etValorLitro.getText().toString().trim();

        if (dataStr.isEmpty() || odoStr.isEmpty() || litrosStr.isEmpty() || valorStr.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos do abastecimento.", Toast.LENGTH_SHORT).show();
            return;
        }

        double hodometroAtual;
        double litros;
        double valorLitro;

        try {
            hodometroAtual = Double.parseDouble(odoStr);
            litros = Double.parseDouble(litrosStr);
            valorLitro = Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Insira valores numéricos válidos.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setLenient(false);
        Date dataAtual;
        try {
            dataAtual = sdf.parse(dataStr);
        } catch (ParseException e) {
            Toast.makeText(this, "Data inválida. Use o formato DD/MM/AAAA.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        float lastOdo = prefs.getFloat(KEY_LAST_ODO, -1f);
        String lastDateStr = prefs.getString(KEY_LAST_DATE, null);

        if (lastOdo == -1f || lastDateStr == null) {
            // Primeiro registro armazenado como linha de base
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(KEY_LAST_ODO, (float) hodometroAtual);
            editor.putString(KEY_LAST_DATE, dataStr);
            editor.apply();

            Toast.makeText(this, "Primeiro registro salvo como base! Cadastre o próximo para ver as análises.", Toast.LENGTH_LONG).show();
            limparCamposAbastecimento();
            return;
        }

        double hodometroAnterior = lastOdo;
        double distancia = hodometroAtual - hodometroAnterior;

        if (distancia <= 0) {
            Toast.makeText(this, "O hodômetro atual deve ser maior que o anterior (" + hodometroAnterior + " km).", Toast.LENGTH_LONG).show();
            return;
        }

        Date dataAnterior;
        try {
            dataAnterior = sdf.parse(lastDateStr);
        } catch (ParseException e) {
            dataAnterior = dataAtual;
        }

        long diffInMillies = Math.abs(dataAtual.getTime() - dataAnterior.getTime());
        long diasDecorridos = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        if (diasDecorridos == 0) {
            diasDecorridos = 1; // Evita divisão por zero se abastecido no mesmo dia
        }

        // Cálculos solicitados
        double consumoMedio = distancia / litros;
        double custoTotal = litros * valorLitro;
        double custoEfetivoKm = custoTotal / distancia;
        double kmPorDia = distancia / diasDecorridos;

        // Exibição dos resultados nas TextViews
        tvDistancia.setText(String.format(Locale.getDefault(), "Distância percorrida: %.2f km", distancia));
        tvConsumoMedio.setText(String.format(Locale.getDefault(), "Consumo médio (km/l): %.2f", consumoMedio));
        tvCustoKm.setText(String.format(Locale.getDefault(), "Custo efetivo (R$/km): R$ %.2f", custoEfetivoKm));
        tvKmDia.setText(String.format(Locale.getDefault(), "Média de km por dia: %.2f km/dia", kmPorDia));

        // Salvar novo registro como base para o próximo cálculo
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_LAST_ODO, (float) hodometroAtual);
        editor.putString(KEY_LAST_DATE, dataStr);
        editor.apply();

        Toast.makeText(this, "Abastecimento registrado com sucesso!", Toast.LENGTH_SHORT).show();
        limparCamposAbastecimento();
    }

    private void limparCamposAbastecimento() {
        etHodometro.setText("");
        etLitros.setText("");
        etValorLitro.setText("");
        preencherDataAtual();
    }

    // Método responsável por resetar o histórico guardado e os textos da tela
    private void limparDadosSalvos() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        limparCamposAbastecimento();
        tvDistancia.setText("Distância percorrida: -");
        tvConsumoMedio.setText("Consumo médio (km/l): -");
        tvCustoKm.setText("Custo efetivo (R$/km): -");
        tvKmDia.setText("Média de km por dia: -");

        Toast.makeText(this, "Histórico limpo! Insira o 1º abastecimento novamente.", Toast.LENGTH_LONG).show();
    }

    private void compararCombustiveis() {
        String etanolStr = etPrecoEtanol.getText().toString().trim();
        String gasolinaStr = etPrecoGasolina.getText().toString().trim();

        if (etanolStr.isEmpty() || gasolinaStr.isEmpty()) {
            Toast.makeText(this, "Informe o preço do etanol e da gasolina.", Toast.LENGTH_SHORT).show();
            return;
        }

        double precoEtanol, precoGasolina;
        try {
            precoEtanol = Double.parseDouble(etanolStr);
            precoGasolina = Double.parseDouble(gasolinaStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Insira valores numéricos válidos para os combustíveis.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (precoGasolina <= 0) {
            Toast.makeText(this, "O preço da gasolina deve ser maior que zero.", Toast.LENGTH_SHORT).show();
            return;
        }

        double razao = precoEtanol / precoGasolina;
        String resultado;

        // Regra dos 70%: Se etanol/gasolina <= 0.70, o etanol é mais vantajoso.
        if (razao <= 0.70) {
            resultado = String.format(Locale.getDefault(), "Abasteça com ETANOL! (Razão: %.1f%% - Mais vantajoso)", razao * 100);
        } else {
            resultado = String.format(Locale.getDefault(), "Abasteça com GASOLINA! (Razão: %.1f%% - Mais vantajoso)", razao * 100);
        }

        tvResultadoComparativo.setText(resultado);
    }
}