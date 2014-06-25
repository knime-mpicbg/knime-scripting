
# Single plot might be better, without lapply
# If possible make selectable controls subset

lapply(9:18, FUN = function (param) {
	quartz()
	#png(filename=paste("LKMSD_rawdata",names(msd[param]),".png",sep=""),width=600,height=500,units="px")
	plot(data=msd,msd[,param]~rownames(msd),subset=c(name=="msd"),main="LKMSD screen END POINT -- raw data", ylab=names(msd[param]),col=colors()[185])
	points(data=msd,msd[,param]~rownames(msd),subset=c(name=="Untreated"),col=colors()[121],pch=21,bg=colors()[121])
	points(data=msd,msd[,param]~rownames(msd),subset=c(name=="DMSO_0.1%"),
		col=colors()[150],pch=21,bg=colors()[150])
	points(data=msd,msd[,param]~rownames(msd),subset=c(name=="Nocodazole_5uM"),
		col=colors()[100],pch=21,bg=colors()[100])
	points(data=msd,msd[,param]~rownames(msd),subset=c(Molename=="TETRANDRINE"),col=colors()[48],pch=22,bg=colors()[48])
	points(data=msd,msd[,param]~rownames(msd),subset=c(Molename=="ASTEMIZOLE"),col=colors()[490],
	pch=22,bg=colors()[490])
	points(data=msd,msd[,param]~rownames(msd),subset=c(Molename=="COLCHICINE"),col=colors()[98],
	pch=22,bg=colors()[98])
	
	legend("topright",pch=c(1,21,21,21,22,22,22),c("msd library","Untreated","DMSO","Nocodazole","Tetrandrine","Astemizole","Colchicine"),col=colors()[c(185,121,150,100,48,490,98)],pt.bg=colors()[c(185,121,150,100,48,490,98)])
#dev.off()
	})
	
screen = in1;
plot(data=screen,screen[,5]~screen[,8]),subset=c(name=="library"),main="my plot", ylab=names(msd[param]
),col=colors()[185])

plot(data=screen,screen[,5]~screen[,8],subset=c(name=="library"),main="my plot", ylab=names(screen[5]),col=colors()[185])

plot(data=screen,screen[,5]~screen[,8],subset=c(name=="library"),main="my plot",col=colors()[185])




#####################

# plot(rownames(subset(screen, treatment=="dmso")),subset(screen, treatment=="dmso")[,14],  main="test", ylab=names(screen)[16], col=colors()[100])

 	points(data=msd,msd[,param]~rownames(msd),subset=c(Molename=="TETRANDRINE"),col=colors()[48],pch=22,bg=colors()[48])


plot(data=screen,screen[,13]~rownames(screen),subset=c(treatment=="library"),main="test2", ylab=names(screen[8]),col=colors()[185])