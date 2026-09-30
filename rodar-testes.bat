@echo off
rem ============================================================================
rem  Rodada de testes e metricas - E-commerce TCC (Microsservicos)
rem
rem  Reproduz o protocolo usado no monolito:
rem    - mvn clean test 3x por servico (MD5 do jacoco.csv = determinismo)
rem    - PITest 2x por servico (comparacao mutante a mutante)
rem    - Branch coverage em contagem absoluta (colunas BRANCH_* do jacoco.csv)
rem
rem  Uso:
rem    rodar-testes.bat            rodada completa (~20 min)
rem    rodar-testes.bat rapido     so 1x mvn clean test, sem PITest (~2 min)
rem
rem  Resultado: resultados-execucao\<MAQUINA>_<DATA>\resumo.txt
rem  Pre-requisitos: JDK 21 (Temurin 21.0.12.1 recomendado) e Maven 3.9+ no PATH
rem ============================================================================
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"

set "MODO=%~1"
if "%MODO%"=="" set "MODO=completo"
if /i "%MODO%"=="completo" (set RODADAS_TESTE=3& set RODADAS_PIT=2) else if /i "%MODO%"=="rapido" (set RODADAS_TESTE=1& set RODADAS_PIT=0) else (
    echo Uso: %~nx0 [completo^|rapido]
    exit /b 1
)

rem ---------------------------------------------------------------- pre-requisitos
where java >nul 2>&1 || (echo [ERRO] Java nao encontrado no PATH. Instale o JDK 21 Temurin. & goto :fim_erro)
where mvn  >nul 2>&1 || (echo [ERRO] Maven nao encontrado no PATH. & goto :fim_erro)

for /f %%t in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "TS=%%t"
set "OUT=%CD%\resultados-execucao\%COMPUTERNAME%_%TS%"
set "RESUMO=%OUT%\resumo.txt"
mkdir "%OUT%" >nul 2>&1

set "JAVA_MVN="
for /f "delims=" %%l in ('call mvn -version 2^>^&1 ^| findstr /c:"Java version"') do set "JAVA_MVN=%%l"
echo !JAVA_MVN! | findstr /c:"21.0.12.1" >nul || (
    echo [AVISO] O Maven nao esta usando o JDK 21.0.12.1 - a referencia do experimento.
    echo         !JAVA_MVN!
    echo         Testes e cobertura nao mudam, mas o total de mutantes do PITest pode mudar.
    echo.
)

> "%RESUMO%" (
    echo RODADA DE TESTES - MICROSSERVICOS
    echo Maquina: %COMPUTERNAME%   Usuario: %USERNAME%   Data: %TS%   Modo: %MODO%
)
for /f "delims=" %%c in ('git rev-parse --short HEAD 2^>nul') do echo Commit: %%c>> "%RESUMO%"
git status --short 2>nul | findstr . >nul && echo [AVISO] Ha alteracoes locais nao commitadas - o resultado pode nao corresponder ao commit acima.>> "%RESUMO%"
call mvn -version >> "%RESUMO%" 2>&1
echo.>> "%RESUMO%"

echo Resultados em: %OUT%
echo.

rem ---------------------------------------------------------------- execucao
set FALHOU=0
for %%s in (user product order report) do call :servico %%s

rem ---------------------------------------------------------------- consolidacao
echo.>> "%RESUMO%"
echo ==== BRANCH COVERAGE - JaCoCo, rodada 1 ====>> "%RESUMO%"
powershell -NoProfile -Command "$t=@{}; foreach($s in 'user','product','order','report'){ $f='%OUT%\'+$s+'-jacoco-1.csv'; if(Test-Path $f){ $r=Import-Csv $f; $bc=($r|Measure-Object BRANCH_COVERED -Sum).Sum; $bm=($r|Measure-Object BRANCH_MISSED -Sum).Sum; $ic=($r|Measure-Object INSTRUCTION_COVERED -Sum).Sum; $im=($r|Measure-Object INSTRUCTION_MISSED -Sum).Sum; $t.bc+=$bc; $t.bm+=$bm; $t.ic+=$ic; $t.im+=$im; '{0,-16} ramos {1}/{2} = {3:N1}%%   instrucoes {4}/{5} = {6:N1}%%' -f ($s+'-service'),$bc,($bc+$bm),(100*$bc/($bc+$bm)),$ic,($ic+$im),(100*$ic/($ic+$im)) } }; if($t.bc -ne $null){ '{0,-16} ramos {1}/{2} = {3:N1}%%   instrucoes {4}/{5} = {6:N1}%%' -f 'AGREGADO',$t.bc,($t.bc+$t.bm),(100*$t.bc/($t.bc+$t.bm)),$t.ic,($t.ic+$t.im),(100*$t.ic/($t.ic+$t.im)) }" >> "%RESUMO%"

if %RODADAS_PIT% gtr 0 (
    echo.>> "%RESUMO%"
    echo ==== MUTATION SCORE - PITest, rodada 1 / determinismo rodada 1 x 2 ====>> "%RESUMO%"
    powershell -NoProfile -Command "$T=@{}; $key={ $_.mutatedClass+'|'+$_.mutatedMethod+'|'+$_.lineNumber+'|'+$_.mutator+'|'+(@($_.indexes.index) -join ',')+'|'+$_.status }; foreach($s in 'user','product','order','report'){ $f1='%OUT%\'+$s+'-mutations-1.xml'; $f2='%OUT%\'+$s+'-mutations-2.xml'; if(Test-Path $f1){ $m=@(([xml](Get-Content -Raw $f1)).mutations.mutation); $n=$m.Count; $k=@($m|Where-Object{$_.detected -eq 'true'}).Count; $nc=@($m|Where-Object{$_.status -eq 'NO_COVERAGE'}).Count; $sv=@($m|Where-Object{$_.status -eq 'SURVIVED'}).Count; $T.n+=$n; $T.k+=$k; $T.nc+=$nc; $T.sv+=$sv; $det='nao comparado'; if(Test-Path $f2){ $a=@($m|ForEach-Object $key|Sort-Object); $b=@(@(([xml](Get-Content -Raw $f2)).mutations.mutation)|ForEach-Object $key|Sort-Object); $det= if(Compare-Object $a $b){'DIFERENTE'}else{'identico'} }; '{0,-16} score {1}/{2} = {3:N1}%%   test strength {1}/{4} = {5:N1}%%   sobreviventes {6}   sem cobertura {7}   2 rodadas: {8}' -f ($s+'-service'),$k,$n,(100*$k/$n),($n-$nc),(100*$k/($n-$nc)),$sv,$nc,$det } }; if($T.n -ne $null){ '{0,-16} score {1}/{2} = {3:N1}%%   test strength {1}/{4} = {5:N1}%%   sobreviventes {6}   sem cobertura {7}' -f 'AGREGADO',$T.k,$T.n,(100*$T.k/$T.n),($T.n-$T.nc),(100*$T.k/($T.n-$T.nc)),$T.sv,$T.nc }" >> "%RESUMO%"
)

echo.>> "%RESUMO%"
echo Referencia - rodada de 2026-09-30, JDK 21.0.12.1:>> "%RESUMO%"
echo   Testes 171/171 = 35+49+63+24  -  Ramos 63/64 = 98,4%%  -  Mutation Score 174/210 = 82,9%%  -  Test Strength 174/204 = 85,3%%>> "%RESUMO%"

echo.
echo ============================================================================
type "%RESUMO%"
echo ============================================================================
echo.
if %FALHOU% neq 0 (
    echo [FALHA] Algum build ou teste falhou - veja os .log em %OUT%
    goto :fim_erro
)
echo Concluido. Envie o arquivo resumo.txt para a equipe:
echo   %RESUMO%
if not defined NOPAUSE pause
exit /b 0

rem ============================================================================
:servico
set "S=%~1"
set "DIR=%S%-service"
set "H1="
echo ==== %DIR% ====>> "%RESUMO%"

for /l %%r in (1,1,%RODADAS_TESTE%) do (
    echo [%DIR%] mvn clean test - rodada %%r de %RODADAS_TESTE% ...
    pushd "%DIR%"
    call mvn -B clean test > "%OUT%\%S%-test-%%r.log" 2>&1
    set RC=!errorlevel!
    popd
    if !RC! neq 0 set FALHOU=1

    set "LINHA=sem resumo de testes - ver log"
    for /f "delims=" %%l in ('findstr /c:"Tests run:" "%OUT%\%S%-test-%%r.log" ^| findstr /v /c:"elapsed"') do set "LINHA=%%l"

    set "H=sem jacoco.csv"
    if exist "%DIR%\target\site\jacoco\jacoco.csv" (
        copy /y "%DIR%\target\site\jacoco\jacoco.csv" "%OUT%\%S%-jacoco-%%r.csv" >nul
        for /f %%h in ('powershell -NoProfile -Command "(Get-FileHash -Algorithm MD5 '%OUT%\%S%-jacoco-%%r.csv').Hash"') do set "H=%%h"
    )
    if not defined H1 set "H1=!H!"
    set "DET=identico a rodada 1"
    if not "!H!"=="!H1!" set "DET=DIFERENTE da rodada 1"
    if %%r==1 set "DET="
    echo   teste %%r: exit=!RC!  !LINHA!  md5=!H! !DET!>> "%RESUMO%"
)

for /l %%r in (1,1,%RODADAS_PIT%) do (
    echo [%DIR%] PITest - rodada %%r de %RODADAS_PIT% ...
    pushd "%DIR%"
    call mvn -B org.pitest:pitest-maven:mutationCoverage > "%OUT%\%S%-pit-%%r.log" 2>&1
    set RC=!errorlevel!
    popd
    if !RC! neq 0 set FALHOU=1
    if exist "%DIR%\target\pit-reports\mutations.xml" copy /y "%DIR%\target\pit-reports\mutations.xml" "%OUT%\%S%-mutations-%%r.xml" >nul
    echo   pitest %%r: exit=!RC!>> "%RESUMO%"
)
exit /b 0

:fim_erro
if not defined NOPAUSE pause
exit /b 1
