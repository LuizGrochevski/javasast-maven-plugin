# javasast-maven-plugin 🔌🛡️

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![License](https://img.shields.io/badge/License-Educational-orange?style=for-the-badge)

Plugin Maven que roda o **[javasast-rs](https://github.com/LuizGrochevski/javasast-rs)** — uma ferramenta SAST em Rust para código Java — como parte do build, falhando o build quando encontra vulnerabilidades a partir de uma severidade configurável.

O plugin é deliberadamente fino: toda a lógica de análise estática vive no binário Rust. Este projeto só invoca o binário com os argumentos certos, repassa a saída pro log do Maven, e converte o exit code do `javasast-rs` numa falha de build controlada (`MojoFailureException`).

## 🚀 Como usar

Adicione ao `pom.xml`, dentro de `<build><plugins>`:

```xml
<plugin>
    <groupId>io.grochevski</groupId>
    <artifactId>javasast-maven-plugin</artifactId>
    <version>0.1.0</version>
    <configuration>
        <binaryPath>/caminho/para/javasast-rs</binaryPath>
        <failOn>high</failOn>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>scan</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Por padrão, roda na fase `verify` (`mvn verify` ou `mvn install` já disparam o scan automaticamente).

Pra rodar avulso, sem alterar o `pom.xml`:

```bash
mvn io.grochevski:javasast-maven-plugin:0.1.0:scan \
  -Djavasast.binaryPath=/caminho/para/javasast-rs \
  -Djavasast.failOn=high
```

## ⚙️ Parâmetros

| Parâmetro | Propriedade | Padrão | Descrição |
|---|---|---|---|
| `binaryPath` | `javasast.binaryPath` | `javasast-rs` | Caminho do binário. Use um caminho absoluto se ele não estiver no `PATH`. |
| `sourceDirectory` | `javasast.sourceDirectory` | `${project.build.sourceDirectory}` | Diretório a ser escaneado. |
| `failOn` | `javasast.failOn` | `high` | `low`, `medium`, `high`, ou `none` (nunca falha o build, só reporta). |
| `outputFormat` | `javasast.outputFormat` | *(nenhum)* | `json`, `markdown`, ou `sarif`, se quiser exportar relatório. |
| `outputFile` | `javasast.outputFile` | `${project.build.directory}/javasast-report` | Caminho base do relatório exportado. |
| `skip` | `javasast.skip` | `false` | Pula o scan sem remover o plugin do `pom.xml`. |

## 📊 Exemplo de saída

**Build limpo:**
```
[INFO] --- javasast:0.1.0:scan (default-cli) @ my-project ---
[INFO] Scanning 3 Java file(s)...
[INFO] No findings — looks clean!
[INFO] javasast-rs scan passed (no findings at or above 'high')
[INFO] BUILD SUCCESS
```

**Build com vulnerabilidade HIGH:**
```
[INFO] [HIGH] .../VulnerableCode.java:93 — URL built via string concatenation... (ssrf)
[INFO] 25 findings total
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal io.grochevski:javasast-maven-plugin:0.1.0:scan: javasast-rs found findings at or above severity 'high'
```

## 🧪 Testado contra

Validado end-to-end contra dois cenários reais: um build limpo (código Quarkus real, sem findings) e um build com vulnerabilidades propositais do **[insecure-java-lab](https://github.com/LuizGrochevski/insecure-java-lab)** (25 findings, build falhou corretamente).

## 🛠️ Build local

```bash
git clone https://github.com/LuizGrochevski/javasast-maven-plugin.git
cd javasast-maven-plugin
mvn clean install
```

Isso instala o plugin no repositório Maven local (`~/.m2`), disponível pra qualquer projeto na máquina.

## 🛣️ Roadmap

- [x] Mojo `scan` bindado à fase `verify`
- [x] Suporte a `--fail-on`, formatos de export, exclusão via `skip`
- [ ] Publicar no Maven Central (hoje só instala local)
- [ ] Baixar/gerenciar o binário `javasast-rs` automaticamente (hoje precisa apontar o caminho manualmente)

## 👨‍💻 Autor

**Luiz Felipe Grochevski** — [LinkedIn](https://www.linkedin.com/in/luiz-felipe-grochevski) | [GitHub](https://github.com/LuizGrochevski)

## ⚠️ Aviso

Este projeto é destinado exclusivamente para fins educacionais e de auditoria de código próprio ou autorizado.

