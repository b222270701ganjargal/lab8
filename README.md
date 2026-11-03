
# Лаборатори №8 — Mockito Mock объектууд ба Mutation Testing

**Хичээл:** Программ хангамжийн чанарын баталгаа ба тест (F.CSA313)  
**Оюутны код:** B222270701  
**Гүйцэтгэсэн:** Г.Ганжаргал  
**Огноо:** 2026-11-03

## 1. Зорилго

JUnit 5 болон Mockito ашиглан хичээлийн бүртгэлийн үйлчилгээний нэгжийн тест бичих, хамаарлуудыг Mock Object-оор орлуулан бизнес логикийг тусгаарлан шалгах, Mutation Testing ашиглан тестүүдийн алдаа илрүүлэх чадварыг үнэлэх.

## 2. Туршилтын орчин

- OS: Windows 11 (amd64)
- Java: OpenJDK 17.0.8.1 (Temurin)
- Maven: Apache Maven 3.10.0
- JUnit Jupiter: 5.10.2
- Mockito: 5.11.0
- Maven Surefire: 3.2.5
- IDE: Visual Studio Code

### java -version

```text
openjdk version "17.0.8.1" 2023-08-24
OpenJDK Runtime Environment Temurin-17.0.8.1+1 (build 17.0.8.1+1)
OpenJDK 64-Bit Server VM Temurin-17.0.8.1+1 (build 17.0.8.1+1, mixed mode, sharing)
```

### mvn -version

```text
Apache Maven 3.10.0
Maven home: C:\tools\apache-maven-3.10.0-bin\apache-maven-3.10.0
Java version: 17.0.8.1, vendor: Eclipse Adoptium
Default locale: en_US, platform encoding: Cp1252
OS name: "windows 11", arch: "amd64"
```

## 3. Туршилтын объект

`CourseRegistrationService` классын `register(studentId, courseId)` методыг JUnit 5 болон Mockito ашиглан шалгав.

`StudentRepository` болон `EmailSender` хамаарлуудыг Mockito Mock объектуудаар орлуулснаар өгөгдлийн сан, бодит и-мэйл үйлчилгээ ашиглахгүйгээр бизнес логикийг шалгах боломжтой болсон.

## 4. Нэгжийн тестүүд (A1–A8)

| № | Туршилт | Хүлээгдэж буй үр дүн |
|---|---|---|
| A1 | Идэвхтэй оюутан хичээлд амжилттай бүртгүүлэх | `OK` |
| A2 | Оюутан олдохгүй | `ERROR_NO_STUDENT` |
| A3 | Идэвхгүй оюутан | `ERROR_INACTIVE_STUDENT` |
| A4 | Өмнөх холбоо хичээл дутуу | `ERROR_PREREQUISITES` |
| A5 | И-мэйл алдаа гарсан ч бүртгэл хүчинтэй | `OK` |
| A6 | `studentId`, `courseId` null | `IllegalArgumentException` |
| A7 | `ArgumentCaptor` ашиглан и-мэйлийн биеийг шалгах | Хичээлийн код агуулсан |
| A8 | `@CsvSource` ашиглан 3 алдааны хувилбар шалгах | Харгалзах алдааны код |

A8 нь 3 оролттой параметржүүлсэн тест учраас нийт 10 тестийн гүйцэтгэлтэй.

### Нэгжийн тестийн үр дүн

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Нотолгоо: `results/mvn-test.txt`

## 5. Mutation Testing

Үндсэн бизнес логикт багшийн заасан гурван өөрчлөлтийг тус тус хийж, тестүүд өөрчлөлтийг илрүүлж байгаа эсэхийг шалгав.

| Мутаци | Өөрчлөлт | Failures | Errors | Үр дүн |
|---|---|---:|---:|---|
| M1 | Идэвхгүй оюутны шалгалтыг комментлох | 2 | 0 | KILLED |
| M2 | `saveRegistration()` дуудалтыг комментлох | 2 | 0 | KILLED |
| M3 | `courseId == null` шалгалтыг арилгах | 1 | 0 | KILLED |

### M1 — Идэвхгүй оюутны шалгалтыг арилгах

`if (!student.active())` шалгалтыг коммент болгосон.

Илрүүлсэн тестүүд:
- `inactiveStudent_returnsErrorWithoutEmail`
- `invalidRegistration_returnsExpectedError`

Нотолгоо: `results/mvn-test-mutant-1.txt`

### M2 — Бүртгэл хадгалах үйлдлийг арилгах

`repository.saveRegistration(studentId, courseId);` мөрийг коммент болгосон.

Тестүүд `verify(repository).saveRegistration(...)` шалгалтаар хадгалах үйлдэл хийгдээгүйг илрүүлсэн.

Илрүүлсэн тестүүд:
- `activeStudent_registrationReturnsOK`
- `emailFailure_registrationRemainsSuccessful`

Нотолгоо: `results/mvn-test-mutant-2.txt`

### M3 — Null шалгалтыг өөрчлөх

Анхны нөхцөл:

`studentId == null || courseId == null`

Өөрчилсөн нөхцөл:

`studentId == null`

`nullInputs_throwExceptionWithoutInteractions` тест уг алдааг илрүүлсэн.

Нотолгоо: `results/mvn-test-mutant-3.txt`

### Mutation Score

Нийт мутаци: 3  
Илрүүлсэн мутаци: 3

**Mutation Score = 3 / 3 × 100 = 100%**

Энэ оноо зөвхөн туршсан гурван мутацид хамаарна.

## 6. Mockito хэрэглээ ба lenient()

Mockito ашиглан хамаарлуудын дуудалтыг `verify()` аргаар шалгасан.

`lenient()` нь тестийн явцад ашиглагдаагүй stubbing-ийг Mockito-ийн strict stubbing шалгалтаас чөлөөлөх боломж олгодог.

Энэ лабораторид шаардлагатай stub-уудыг тест тус бүрд тодорхойлсон тул `lenient()` хэрэглэх шаардлагагүй болсон.

## 7. Дүгнэлт

Mockito-ийн Mock объектууд нь гаднын хамаарлуудыг тусгаарлаж, бизнес логикийг бие даан шалгах боломж олгодог. Fake объект нь ажиллах боломжтой хялбаршуулсан хэрэгжүүлэлттэй байдаг бол Mock объект нь харилцан үйлчлэлийг шалгахад тохиромжтой. `verify()` ашигласнаар методын буцаах утгаас гадна бүртгэл хадгалах болон и-мэйл илгээх үйлдэл хийгдсэн эсэхийг баталгаажуулсан.

`ArgumentCaptor` ашиглан и-мэйлийн биеийн бодит аргументыг шалгасан. `@CsvSource` ашиглан гурван алдааны хувилбарыг давтагдсан код багатайгаар шалгав. Нийт 10 тест амжилттай ажилласан.

Mutation Testing-ийн явцад бизнес логикийн гурван зориудын өөрчлөлтийг тестүүд бүгдийг илрүүлсэн. Туршилтын дараа үндсэн кодыг анхны хувилбарт сэргээж, бүх тестийг дахин амжилттай ажиллуулсан.

Бизнес логикийг агуулж буй CourseRegistrationService классыг өөрийг нь mock хийх ёсгүй, учир нь бид яг энэ классын бодит ажиллагааг шалгах зорилготой. verify()-г хэт олон дотоод дуудлагад ашиглавал тест хэрэгжүүлэлтийн нарийн бүтэцтэй хэт хамааралтай болж, бизнесийн үр дүн өөрчлөгдөөгүй байхад ч шаардлагагүйгээр унах эрсдэлтэй.