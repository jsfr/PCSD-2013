A = load('data/benchmark.local.dat');
B = [];
C = [];

D = load('data/benchmark.remote.dat');
E = [];
F = [];

for j=A(1,1):A(1,1):A(end,1),
    B(end+1,:) = mean(A(A(:,1)==j,:));
    C(end+1,:) = std(A(A(:,1)==j,:));
end

for j=D(1,1):D(1,1):D(end,1),
    E(end+1,:) = mean(D(D(:,1)==j,:));
    F(end+1,:) = std(D(D(:,1)==j,:));
end

W = 4; H = 3;

h = figure(1);
e = errorbar(B(:,1),B(:,2),C(:,2));
e = errorbar(E(:,1),E(:,2),F(:,2));
set(e,'Marker','*');
set(e,'linewidth',3);
title('Throughput');
xlabel('# of workers');
ylabel('succXact / s');
set(h,'PaperUnits','inches')
set(h,'PaperOrientation','portrait');
set(h,'PaperSize',[H,W]);
set(h,'PaperPosition',[0,0,W,H]);
FS = findall(h,'-property','FontWeight');
set(FS,'FontWeight','bold');
grid on;
print(h, '-dpng','images/throughput.png');

h = figure(2);
e = errorbar(B(:,1),B(:,3),C(:,3));
e = errorbar(E(:,1),E(:,3),F(:,3));
set(e,'Marker','*');
set(e,'linewidth',3);
title('Latency');
xlabel('# of workers');
ylabel('seconds');
set(h,'PaperUnits','inches')
set(h,'PaperOrientation','portrait');
set(h,'PaperSize',[H,W])
set(h,'PaperPosition',[0,0,W,H]);
FS = findall(h,'-property','FontWeight');
set(FS,'FontWeight','bold');
grid on;
print(h, '-dpng','images/latency.png');